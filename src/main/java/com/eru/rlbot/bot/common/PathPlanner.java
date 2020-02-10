package com.eru.rlbot.bot.common;

import com.eru.rlbot.bot.optimizer.CarBallOptimizer;
import com.eru.rlbot.bot.strats.BallPredictionUtil;
import com.eru.rlbot.common.input.*;
import com.eru.rlbot.common.vector.Vector3;
import com.eru.rlbot.common.vector.Vector3s;
import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rlbot.flat.BallPrediction;
import rlbot.flat.PredictionSlice;
import java.util.Optional;

public class PathPlanner {

  private static final Logger logger = LogManager.getLogger("PathPlanner");

  public static Optional<Path> doShotPlanning(DataPacket input) {
    long startTime = System.nanoTime();
    for (BallPredictionUtil.ExaminedBallData examinedBall : BallPredictionUtil.getPredictions()) {
      if (examinedBall.ball.position.z > 200 || !examinedBall.isHittable().orElse(Boolean.TRUE)) {
        continue;
      }

      Path path = planShotOnGoal(input, examinedBall.ball);

      // TODO: Check a slower traverse time to use less boost.
      Plan plan = path.fastestTraverseTime(0, input.car.boost);
      if (plan.traverseTime * 1.05 < examinedBall.ball.elapsedSeconds - input.car.elapsedSeconds) {
        logger.debug(String.format("Took %f ms to plan", (System.nanoTime() - startTime) / 1000000d));
        examinedBall.addPath(path);
        return Optional.of(path); // TODO: Remove
      }
    }

    BallPredictionUtil.ExaminedBallData firstHittable = BallPredictionUtil.getFirstHittableLocation();
    if (firstHittable == null) {
      logger.debug("Return default value");
      return Optional.empty();
    }
    return Optional.of(firstHittable.getPath());
  }

  public static Path doDefensePlanning(DataPacket input) {
    Optional<BallPrediction> optionalBallInfo = DllHelper.getBallPrediction();
    if (!optionalBallInfo.isPresent()) {
      return carBallPath(input);
    }

    BallPrediction prediction = optionalBallInfo.get();
    for (int i = 0; i < prediction.slicesLength(); i += 5) {
      PredictionSlice predictionSlice = prediction.slices(i);
      if (predictionSlice.physics().location().z() > 110) {
        continue;
      }

      Path path = planDefense(input, predictionSlice);
      double timeToBall = path.minimumTraverseTime();
      if (timeToBall + input.car.elapsedSeconds < predictionSlice.gameSeconds()) {
        return path;
      }
    }

    return carBallPath(input);
  }

  private static Path carBallPath(DataPacket input) {
    return Path.builder()
        .setStartingCar(input.car)
        .setTargetCar(CarBallOptimizer.getOptimalApproach(input.ball, input.ball.position.plus(Angles.carBall(input).toMagnitude(500))))
        .addEarlierSegment(Segment.straight(input.car.position, input.ball.position))
        .build();
  }

  private static Path planDefense(DataPacket input, PredictionSlice predictionSlice) {
    BallData ballLocation = BallData.fromPredictionSlice(predictionSlice);

    CarData optimalCar = CarBallOptimizer.getOptimalApproach(ballLocation, Goal.opponentGoal(input.car.team).center);

    return planPath(input.car, optimalCar);
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

  private static CarData fastPlan(CarData car, BallData ball) {
    Circle turnToBall = Paths.closeTurningRadius(ball.position, car);
    Paths.TangentPoints tangentPoints = Paths.tangents(turnToBall, ball.position);

    Vector3 approachSpot = turnToBall.isClockwise(car) ? tangentPoints.right : tangentPoints.left;
    Vector3 approachBall = ball.position.minus(approachSpot);

    Orientation orientation = Orientation.fromFlatVelocity(approachBall);

    return CarData.builder()
        .setTime(ball.elapsedSeconds)
        .setOrientation(orientation)
        .setVelocity(approachBall.toMagnitude(Math.max(car.groundSpeed, 1500)))
        .setPosition(makeGroundCar(orientation, ball.position))
        .build();
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
        logger.warn("Cannot plan jump for height: %f", workingTarget.position.z);
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

//    Paths.CircleTangents tangents = Paths.tangents(closeTurningRadius, closeApproachCircle);
//    Segment connectingSegment;
//    if (closeApproachCircle.isClockwise(workingTarget)) {
//      connectingSegment = closeTurningRadius.isClockwise(car) ? tangents.cwcw : tangents.ccwcw;
//    } else {
//      connectingSegment = closeTurningRadius.isClockwise(car) ? tangents.cwccw : tangents.ccwccw;
//    }
//
//    Segment approachArc =
//        Segment.arc(connectingSegment.end, workingTarget.position, closeApproachCircle, closeApproachCircle.isClockwise(workingTarget));
//    if (!addPathSegment(car, approachArc, pathBuilder)) {
//      return pathBuilder
//          .addEarlierSegment(Segment.arc(car.position, workingTarget.position, closeApproachCircle, closeApproachCircle.isClockwise(workingTarget)))
//          .build();
//    }
//
//    if (!addPathSegment(car, connectingSegment, pathBuilder)) {
//      return pathBuilder
//          .addEarlierSegment(Segment.straight(car.position, connectingSegment.end))
//          .build();
//    }
//
//    return pathBuilder
//        .addEarlierSegment(Segment.arc(car.position, connectingSegment.start, closeTurningRadius, closeTurningRadius.isClockwise(car)))
//        .build();
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
