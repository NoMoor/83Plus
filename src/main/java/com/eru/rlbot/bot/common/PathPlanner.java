package com.eru.rlbot.bot.common;

import com.eru.rlbot.bot.optimizer.CarBallOptimizer;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;
import com.eru.rlbot.common.vector.Vector3s;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rlbot.flat.BallPrediction;
import rlbot.flat.PredictionSlice;
import java.util.Optional;

public class PathPlanner {

  private static final Logger logger = LogManager.getLogger("PathPlanner");

  public static Path doShotPlanning(DataPacket input) {
    Optional<BallPrediction> optionalBallInfo = DllHelper.getBallPrediction();
    if (!optionalBallInfo.isPresent()) {
      return carBallPath(input);
    }

    BallPrediction prediction = optionalBallInfo.get();
    // TODO: Don't step through every location.
    for (int i = 0; i < prediction.slicesLength(); i++) {
      PredictionSlice predictionSlice = prediction.slices(i);
      if (predictionSlice.physics().location().z() > 110) {
        continue;
      }

      Path path = planShotOnGoal(input, predictionSlice);
      double timeToBall = path.minimumTraverseTime();
      if (true || timeToBall + input.car.elapsedSeconds < predictionSlice.gameSeconds()) { // TODO: Undo
        return path;
      }
    }

    return carBallPath(input);
  }

  public static Path doDefensePlanning(DataPacket input) {
    Optional<BallPrediction> optionalBallInfo = DllHelper.getBallPrediction();
    if (!optionalBallInfo.isPresent()) {
      return carBallPath(input);
    }

    BallPrediction prediction = optionalBallInfo.get();
    // TODO: Don't step through every location.
    for (int i = 0; i < prediction.slicesLength(); i++) {
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
        .addEarlierSegment(Path.Segment.straight(input.car.position, input.ball.position))
        .build();
  }

  private static Path planDefense(DataPacket input, PredictionSlice predictionSlice) {
    BallData ballLocation = BallData.fromPredictionSlice(predictionSlice);

    CarData optimalCar = CarBallOptimizer.getOptimalApproach(ballLocation, Goal.opponentGoal(input.car.team).center);

    return planPath(input.car, optimalCar);
  }

  private static Path planShotOnGoal(DataPacket input, PredictionSlice predictionSlice) {
    return plan(input.car, BallData.fromPredictionSlice(predictionSlice), Goal.opponentGoal(input.car.team).center);
  }

  private static Path plan(CarData currentCar, BallData targetBall, Vector3 target) {
    CarData optimalCar = CarBallOptimizer.getOptimalApproach(targetBall, target);

    return planPath(currentCar, optimalCar);
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

        if (!addPathSegment(car, Path.Segment.jump(tempTarget.position, workingTarget.position), pathBuilder)) {
          return pathBuilder
              .addEarlierSegment(Path.Segment.jump(car.position, workingTarget.position))
              .build();
        }

        workingTarget = tempTarget;
      } else {
        logger.warn("Cannot plan jump for height: %f", workingTarget.position.z);
      }
    }

    CarData projectedCardData = CarDataUtils.rewindDistance(workingTarget, 300);
    if (!addPathSegment(car, Path.Segment.straight(projectedCardData.position, workingTarget.position), pathBuilder)) {
      return pathBuilder
          .addEarlierSegment(Path.Segment.straight(car.position, workingTarget.position))
          .build();
    }

    workingTarget = projectedCardData;

    Circle closeApproachCircle = Paths.closeApproach(workingTarget, car);
    Vector3 tangentPoint = Paths.tangent(closeApproachCircle, car.position, workingTarget);

//    // TODO: Probably just get rid of this code.
//    boolean insideApproachCircle = car.position.distance(closeApproachCircle.center) < closeApproachCircle.radius * .8;
//    boolean isTravelingTowardGoal = Math.signum(car.velocity.dot(workingTarget.velocity)) > 0;
//
//    if (isTravelingTowardGoal && insideApproachCircle) {
//      return pathBuilder
//          .addEarlierSegment(Path.Segment.arc(car.position, workingTarget, closeApproachCircle))
//          .build();
//    }

    double tangentCorrection = Angles.flatCorrectionAngle(car, tangentPoint);
    double approachCorrection = Angles.flatCorrectionAngle(car, workingTarget.position);

    if (Math.signum(tangentCorrection) == Math.signum(approachCorrection)
        && Math.abs(tangentCorrection) < Math.abs(approachCorrection)) {

      Circle circle = getCircle(car, workingTarget);
      return pathBuilder
          .addEarlierSegment(Path.Segment.arc(car.position, workingTarget, circle))
          .build();
    }

    return pathBuilder
        .addEarlierSegment(Path.Segment.arc(tangentPoint, workingTarget, closeApproachCircle))
        .addEarlierSegment(Path.Segment.straight(car.position, tangentPoint))
        .build();
  }

  // TODO: I don't think this is quite right.
  // Calculates a circle from two tangent orientations.
  private static Circle getCircle(CarData a, CarData b) {
    // Do one gradual arc to the approach.
    double correctionAngle = a.orientation.getNoseVector().flatten()
        .correctionAngle(b.orientation.getNoseVector().flatten());
    double halfAngle = Math.signum(correctionAngle) * (Math.PI - Math.abs(correctionAngle)) / 2;

    // TODO: Add check if halfAngle == 0 and a/b are not co-linear.

    double centerABisectorAngle = Math.abs(halfAngle);

    Vector3 ray = b.position.minus(a.position);
    Vector3 halfRay = ray.divide(2);
    Vector3 bisectorPoint = halfRay.plus(a.position);

    // tan(theta) = opposite / adjacent
    // tan(theta) * adjacent = opposite;
    double middleAdjustmentMagnitude = Math.tan(centerABisectorAngle) * halfRay.magnitude();

    Vector3 centerAdjustment = Vector3.of(-Math.signum(halfAngle) * ray.y, ray.x, 0).toMagnitude(middleAdjustmentMagnitude);
    Vector3 centerPosition = bisectorPoint.plus(centerAdjustment);

    double radius = a.position.minus(centerPosition).magnitude();

//    BotRenderer botRenderer = BotRenderer.forIndex(a.playerIndex);
//    botRenderer.renderHitBox(b);
//    botRenderer.renderPoint(Color.PINK, a.position.plus(halfRay), 5);
//    botRenderer.renderProjection(a, b.position, Color.magenta);
//    botRenderer.renderProjection(a, bisectorPoint, Color.orange);
//
//    botRenderer.renderProjection(a, a.orientation.getNoseVector().flatten().scaledToMagnitude(3000).asVector3().plus(a.position), Color.red);
//    botRenderer.renderProjection(b, b.orientation.getNoseVector().flatten().scaledToMagnitude(-3000).asVector3().plus(b.position), Color.red);
//    botRenderer.addAlertText("Test %f %f", correctionAngle, halfAngle);

    return new Circle(centerPosition, radius);
  }

  private static boolean addPathSegment(CarData car, Path.Segment segment, Path.Builder pathBuilder) {
    if (isOnSegment(car, segment)) {
      return false;
    }

    pathBuilder.addEarlierSegment(segment);
    return true;
  }

  private static boolean isOnSegment(CarData car, Path.Segment segment) {
    // TODO: Test this.
    // Velocity is going toward end but not toward start.
    Vector3 carToEnd = segment.end.minus(car.position);
    Vector3 carToStart = segment.start.minus(car.position);

    boolean goingTowardEnd = carToEnd.dot(car.velocity) > 0;
    boolean goingTowardStart = carToStart.dot(car.velocity) > 0;

    return goingTowardEnd
        && !goingTowardStart
        && Vector3s.shortestDistanceToVector(car.position, segment.start, segment.end) < 100;
  }

  private static final float BUFFER = Constants.CAR_HEIGHT;

  private static boolean requiresJump(CarData car) {
    boolean isOnSideWall = Math.abs(car.position.x) > Constants.HALF_WIDTH - BUFFER;
    boolean isOnBackWall = Math.abs(car.position.y) > Constants.HALF_LENGTH - BUFFER;

    return !isOnSideWall && !isOnBackWall && car.position.z > 30;
  }
}
