package com.eru.rlbot.bot.path;

import com.eru.rlbot.bot.common.Accels;
import com.eru.rlbot.bot.common.CarDataUtils;
import com.eru.rlbot.bot.common.Circle;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.BoundingBox;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.vector.Vector3;
import com.eru.rlbot.common.vector.Vector3s;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PathPlanner {

  private static final Logger logger = LogManager.getLogger("PathPlanner");

  public static Optional<Plan> getGroundPath(CarData car, BallData ball) {
    if (ball.position.z > 200) {
      return Optional.empty();
    }

    CarData closestHit = closestStrike(car, Moment.from(ball));
    Path path = planPath(car, closestHit);
    Plan plan = path.minGroundTime(car.boost);

    if (plan.traverseTime < ball.time - car.elapsedSeconds) {
      return Optional.of(plan);
    } else {
      return Optional.empty();
    }
  }

  public static Path oneTurn(CarData car, Moment moment) {
    CarData targetCar = closestStrike(car, moment);
    return planPath(car, targetCar);
  }

  public static Path oneTurn(CarData car, BallData ball) {
    return oneTurn(car, Moment.from(ball));
  }

  // TODO: Include how far max left-right and closest to the middle point as possible for optimization.
  public static CarData closestStrike(CarData car, Moment moment) {
    Circle turnToBall = Paths.closeTurningRadius(moment.position, car);
    Paths.TangentPoints tangentPoints = Paths.tangents(turnToBall, moment.position); // TODO: Turn moment.position into a circle range.

    Vector3 approachSpot = turnToBall.isClockwise(car) ? tangentPoints.right : tangentPoints.left;
    Vector3 approachBall = moment.position.minus(approachSpot);

    Orientation orientation = Orientation.fromFlatVelocity(approachBall);

    Vector3 position = makeGroundCar(orientation, moment);
    Vector3 velocity = approachBall.toMagnitude(Constants.BOOSTED_MAX_SPEED);

    return CarData.builder()
        .setTime(moment.time)
        .setOrientation(orientation)
        .setVelocity(velocity)
        .setPosition(position)
        .build();
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

    // TODO: Handle when the car is very close to the ball.

    if (requiresJump(targetCar)) {
      // TODO: Make this work for walls.
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

    CarData projectedCardData = CarDataUtils.rewindDistance(workingTarget, 300);
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
