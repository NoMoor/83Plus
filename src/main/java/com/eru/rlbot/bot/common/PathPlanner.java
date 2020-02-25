package com.eru.rlbot.bot.common;

import com.eru.rlbot.bot.optimizer.CarBallOptimizer;
import com.eru.rlbot.bot.strats.BallPredictionUtil;
import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.common.Lists;
import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.BoundingBox;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.vector.Vector3;
import com.eru.rlbot.common.vector.Vector3s;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PathPlanner {

  private static final Logger logger = LogManager.getLogger("PathPlanner");

  public static Optional<Path> doShotPlanning(DataPacket input) {
    BallPredictionUtil ballPredictionUtil = BallPredictionUtil.forIndex(input.car.playerIndex);

    int frames = 0;
    long startTime = System.nanoTime();
    List<BallPredictionUtil.ExaminedBallData> ballPredictions =
        Lists.everyNth(ballPredictionUtil.getPredictions(), 5);

    // Check if we can even get to the ball.
    for (BallPredictionUtil.ExaminedBallData examinedBall : ballPredictions) {
      if (examinedBall.ball.position.z > 200 || examinedBall.isHittableBy().isPresent()) {
        continue;
      }

      CarData fastPlan = fastPlan(input.car, examinedBall);
      Path path = planPath(input.car, fastPlan);
      Plan time = path.fastestTraverseTime(0, input.car.boost);
      if (time.traverseTime < examinedBall.ball.time - input.car.elapsedSeconds) {
        examinedBall.addPath(path);
        examinedBall.addFastPlan(time);
        examinedBall.setHittableBy(Tactic.TacticType.STRIKE);
      } else {
//        examinedBall.setHittable(false);
      }
    }

    List<BallPredictionUtil.ExaminedBallData> hittablePositions =
        ballPredictions.stream()
            .filter(ball -> ball.isHittableBy().isPresent())
            .collect(Collectors.toList());

    for (BallPredictionUtil.ExaminedBallData hittableBall : hittablePositions) {
      frames++;
      Path path = planShotOnGoal(input, hittableBall.ball);

      // TODO: Check a slower traverse time to use less boost.
      Plan plan = path.fastestTraverseTime(0, input.car.boost);
      if (plan.traverseTime * 1.05 < hittableBall.ball.time - input.car.elapsedSeconds) {
        logger.debug(String.format("Took %f ms to plan %d frames", (System.nanoTime() - startTime) / 1000000d, frames));
        hittableBall.addPath(path);
        return Optional.of(path); // TODO: Remove
      }
    }

    BallPredictionUtil.ExaminedBallData firstHittable = ballPredictionUtil.getFirstHittableLocation();
    if (firstHittable == null) {
      logger.debug("Return default value");
      return Optional.empty();
    }
    return Optional.ofNullable(firstHittable.getPath());
  }

  private static Path planShotOnGoal(DataPacket input, BallData ball) {
    return plan(input.car, ball, Goal.opponentGoal(input.car.team).centerTop.addZ(-Constants.BALL_RADIUS));
  }

  private static Path plan(CarData currentCar, BallData targetBall, Vector3 target) {
    // Fast Plan
    CarData approximateApproach = fastPlan(currentCar, targetBall);

    CarData optimalCar = CarBallOptimizer.getOptimalApproach(targetBall, target, approximateApproach);

    return planPath(currentCar, optimalCar);
  }

  public static Path fastPath(CarData car, Moment moment) {
    CarData targetCar = fastPlan(car, moment);
    return planPath(car, targetCar);
  }

  public static Path fastPath(CarData car, BallData ball) {
    return fastPath(car, Moment.from(ball));
  }

  private static CarData fastPlan(CarData car, BallPredictionUtil.ExaminedBallData ball) {
    CarData fastPlan = fastPlan(car, ball.ball);
    ball.setFastTarget(fastPlan);
    return fastPlan;
  }

  private static CarData fastPlan(CarData car, Moment moment) {
    // TODO: This should return a Path.
    Circle turnToBall = Paths.closeTurningRadius(moment.position, car);
    Paths.TangentPoints tangentPoints = Paths.tangents(turnToBall, moment.position);

    Vector3 approachSpot = turnToBall.isClockwise(car) ? tangentPoints.right : tangentPoints.left;
    Vector3 approachBall = moment.position.minus(approachSpot);

    Orientation orientation = Orientation.fromFlatVelocity(approachBall);

    return CarData.builder()
        .setTime(moment.time)
        .setOrientation(orientation)
        .setVelocity(approachBall.toMagnitude(Math.max(car.groundSpeed, 1500)))
        .setPosition(makeGroundCar(orientation, moment.position))
        .build();
  }

  private static CarData fastPlan(CarData car, BallData ball) {
    return fastPlan(car, Moment.from(ball));
  }

  private static Vector3 makeGroundCar(Orientation orientation, Vector3 ballPosition) {
    double collisionHeightOffset = -(Constants.BALL_RADIUS - BoundingBox.height); // Assumes the ball and car are resting on the same surface.
    double collisionLengthOffset = Math.cos(Math.atan(collisionHeightOffset / Constants.BALL_RADIUS)) * Constants.BALL_RADIUS;

    double verticalOffset = Constants.BALL_RADIUS - (BoundingBox.height - Constants.CAR_AT_REST);
    double carLengthOffset = BoundingBox.RJ_OFFSET.x + BoundingBox.halfLength;

    Vector3 offset = Vector3.of(collisionLengthOffset + carLengthOffset, 0, verticalOffset);
    return ballPosition.minus(orientation.getOrientationMatrix().dot(offset));
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

    boolean goingTowardEnd = carToEnd.dot(car.velocity) > 0;
    boolean goingTowardStart = carToStart.dot(car.velocity) > 0;

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
