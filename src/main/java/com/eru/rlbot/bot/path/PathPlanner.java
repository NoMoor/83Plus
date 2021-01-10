package com.eru.rlbot.bot.path;

import com.eru.rlbot.bot.common.Accels;
import com.eru.rlbot.bot.common.CarDataUtils;
import com.eru.rlbot.bot.common.Circle;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.common.Matrix3;
import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.BoundingBox;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.vector.Vector3;
import com.eru.rlbot.common.vector.Vector3s;
import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PathPlanner {

  private static final Logger logger = LogManager.getLogger("PathPlanner");

  public static Optional<Plan> getGroundPath(CarData car, BallData ball) {
    if (ball.position.z > 300) {
      return Optional.empty();
    }

    CarData groundCar;
    if (car.position.z < 25) {
      groundCar = car;
    } else {
      groundCar = projectCarToGround(car);
    }

    Optional<CarData> closestHit = closestStrike(groundCar, Moment.from(ball));
    if (!closestHit.isPresent()) {
      return Optional.empty();
    }

    Path path = planPath(groundCar, closestHit.get());

    double timeToBall = ball.time - groundCar.elapsedSeconds;
    double absoluteMinTime = path.length() / Constants.BOOSTED_MAX_SPEED;
    if (absoluteMinTime > timeToBall) {
      return Optional.empty();
    }

    Plan plan = path.minGroundTime(groundCar.boost);

    if (plan.traverseTime < timeToBall) {
      return Optional.of(plan);
    } else {
      return Optional.empty();
    }
  }

  // Assumes car is flying in air. Doesn't really handle wall play.
  private static CarData projectCarToGround(CarData car) {
    Vector3 velocity = car.velocity;
    double height = car.position.z;
    double timeToGround = Accels.floatingTimeToHeight(velocity.z, height, Constants.CAR_AT_REST);

    Vector3 newPosition = velocity.flat().multiply(timeToGround).plus(car.position.flat()).setZ(Constants.CAR_AT_REST);

    return car.toBuilder()
        .setTime(car.elapsedSeconds + timeToGround)
        .setPosition(newPosition)
        .setVelocity(velocity.flat())
        .setOrientation(Orientation.fromFlatVelocity(velocity))
        .build();
  }

  public static Path oneTurn(CarData car, Moment moment) {
    Optional<CarData> targetCar = closestStrike(car, moment);
    return targetCar.map(carData -> planPath(car, carData)).orElse(null);
  }

  public static Path oneTurn(CarData car, BallData ball) {
    return oneTurn(car, Moment.from(ball));
  }

  // TODO: Include how far max left-right and closest to the middle point as possible for optimization.
  public static Optional<CarData> closestStrike(CarData car, Moment moment) {
    Circle turnToBall = Paths.closeTurningRadius(moment.position, car);
    Paths.TangentPoints tangentPoints = Paths.tangents(turnToBall, moment.position);

    // If there is a turn - straight path using circle tangent, use that.
    if (tangentPoints.exist()) {
      Vector3 approachSpot = turnToBall.isClockwise(car) ? tangentPoints.right : tangentPoints.left;
      Vector3 approachBall = moment.position.minus(approachSpot);

      Orientation orientation = Orientation.fromFlatVelocity(approachBall);

      Vector3 position = makeGroundCar(orientation, moment);
      double speed = Accels.boostedTimeToDistance(
          car.boost * .5, car.groundSpeed, Math.max(approachBall.magnitude() - 300, 0)).getSpeed();
      Vector3 velocity = approachBall.toMagnitude(speed);

      return Optional.of(CarData.builder()
          .setTime(moment.time)
          .setOrientation(orientation)
          .setVelocity(velocity)
          .setPosition(position)
          .build());
    }

    // Check to see if the ball circle and the circle traced by the front corner of the car intersect.
    Circle closeTracedCircle = Paths.closeTracedRadius(moment.position, car);
    Circle targetCircle = Circle.fromMoment(moment);
    double centerDistance = targetCircle.center.distance(closeTracedCircle.center);
    if (centerDistance < 1 || centerDistance < closeTracedCircle.radius - targetCircle.radius) {
      // The ball is inside the turning radius and cannot be hit.
      return Optional.empty();
    }

    // https://stackoverflow.com/questions/3349125/circle-circle-intersection-points
    Optional<Vector3> intersectionPoint = closeTracedCircle.intersections(targetCircle).stream()
        .min(Comparator.comparing(point -> Segment.calculateArcLength(car.position, point, closeTracedCircle, car)));

    // Cannot hit without slowing or powersliding.
    if (!intersectionPoint.isPresent()) {
      return Optional.empty();
    }

    Vector3 intersection = intersectionPoint.get();

    boolean isClockWise = closeTracedCircle.isClockwise(car);
    Vector3 position = positionFromIntersectionCorner(intersection, closeTracedCircle, isClockWise);
    double speed = Math.max(car.groundSpeed, 800);
    Vector3 velocity = getFrontToBackAngle(intersection, closeTracedCircle, isClockWise)
        .toMagnitude(-speed);
    Orientation orientation = Orientation.fromFlatVelocity(velocity);

    CarData intersectionCar = car.toBuilder()
        .setPosition(position)
        .setVelocity(velocity)
        .setOrientation(orientation)
        .build();

    return Optional.of(intersectionCar);
  }

  private static final Matrix3 ccwRotation = Orientation.convert(0, -.26, 0).getOrientationMatrix();
  private static final Matrix3 cwRotation = Orientation.convert(0, .26, 0).getOrientationMatrix();

  private static Vector3 getFrontToBackAngle(Vector3 intersection, Circle circle, boolean isClockwise) {
    return intersection.minus(circle.center)
        .dot(isClockwise ? cwRotation : ccwRotation)
        .counterClockwisePerpendicular()
        .toMagnitude(isClockwise ? 1 : -1);
  }

  private static Vector3 positionFromIntersectionCorner(Vector3 intersection, Circle circle, boolean isClockwise) {
    Vector3 frontToBack = getFrontToBackAngle(intersection, circle, isClockwise);

    Vector3 lrOffset = frontToBack.counterClockwisePerpendicular()
        .toMagnitude((isClockwise ? -1 : 1) * BoundingBox.halfWidth);
    Vector3 fbOffset = frontToBack.toMagnitude(BoundingBox.frontToRj);

    return intersection.plus(lrOffset).plus(fbOffset);
  }

  private static Vector3 makeGroundCar(Orientation orientation, Moment target) {
    Vector3 offset = Vector3.zero();

    if (target.type == Moment.Type.BALL) {
      double collisionHeightOffset = -(Constants.BALL_RADIUS - BoundingBox.height); // Assumes the ball and car are resting on the same surface.;
      double collisionLengthOffset = Math.cos(Math.atan(collisionHeightOffset / Constants.BALL_RADIUS)) * Constants.BALL_RADIUS;

      double verticalOffset = Constants.BALL_RADIUS - (BoundingBox.height - Constants.CAR_AT_REST);
      double carLengthOffset = BoundingBox.RJ_OFFSET.x + BoundingBox.halfLength;
      offset = Vector3.of(collisionLengthOffset + carLengthOffset, 0, verticalOffset);
    }

    return target.position.minus(orientation.getOrientationMatrix().dot(offset));
  }

  // TODO: Pick up boosts.
  // TODO: Add in U-turn / drift.
  public static Path planPath(CarData car, CarData targetCar) {
    Path.Builder pathBuilder = Path.builder()
        .setStartingCar(car)
        .setTargetCar(targetCar);

    CarData workingTarget = targetCar;

    // This seems to jump too often.
    if (requiresJump(targetCar)) {
      Optional<Float> time = Accels.jumpTimeToHeight(workingTarget.position.z);
      if (time.isPresent()) {
        CarData tempTarget = CarDataUtils.rewindTime(workingTarget, time.get());
        tempTarget = tempTarget.toBuilder()
            .setPosition(Vector3.of(tempTarget.position.x, tempTarget.position.y, Constants.CAR_AT_REST))
            .build();

        if (!addPathSegment(car, Segment.jump(tempTarget.position, workingTarget.position), pathBuilder)) {
          return pathBuilder
              .addEarlierSegment(Segment.jump(car.position, workingTarget.position))
              .build();
        }

        workingTarget = tempTarget;
      } else {
        logger.debug("Cannot plan jump for height: {}", workingTarget.position.z);
      }
    }

    CarData projectedCardData = CarDataUtils.rewindDistance(workingTarget, 250);
    if (!addPathSegment(car, Segment.straight(projectedCardData.position, workingTarget.position), pathBuilder)) {
      // TODO: Fix this...
      return pathBuilder
          .addEarlierSegment(Segment.straight(car.position, workingTarget.position))
          .build();
    }

    workingTarget = projectedCardData;

    ImmutableList<Segment> shortestBiArc = Paths.shortestBiArc(workingTarget, car);

    return pathBuilder.addEarlierSegments(shortestBiArc)
        .build();
  }

  private static boolean addPathSegment(CarData car, Segment segment, Path.Builder pathBuilder) {
    if (isOnSegment(car, segment)) {
      return false;
    }

    pathBuilder.addEarlierSegment(segment);
    return true;
  }

  private static boolean isOnSegment(CarData car, Segment segment) {
    // TODO: Test this.
    // Velocity is going toward end but not toward start.
    Vector3 carToEnd = segment.end.minus(car.position);
    Vector3 carToStart = segment.start.minus(car.position);

    boolean goingTowardEnd = carToEnd.dot(car.orientation.getNoseVector()) > 0;
    boolean goingTowardStart = carToStart.dot(car.orientation.getNoseVector()) > 0;

    return goingTowardEnd
        && !goingTowardStart
        && Vector3s.shortestDistanceToVector(car.position, segment.start, segment.end) < 150;
  }

  private static final float BUFFER = Constants.CAR_HEIGHT;

  private static boolean requiresJump(CarData car) {
    boolean isOnSideWall = Math.abs(car.position.x) > Constants.HALF_WIDTH - BUFFER;
    boolean isOnBackWall = Math.abs(car.position.y) > Constants.HALF_LENGTH - BUFFER;

    return !isOnSideWall && !isOnBackWall && car.position.z > 30;
  }
}
