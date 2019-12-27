package com.eru.rlbot.bot.common;

import com.eru.rlbot.bot.optimizer.CarBallOptimizer;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.flat.BallPrediction;
import rlbot.flat.PredictionSlice;
import java.util.Optional;

public class PathPlanner {

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
      if (timeToBall + input.car.elapsedSeconds < predictionSlice.gameSeconds()) {
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
    return new Path(input.car, input.car, new Path.Segment(input.car.position, input.ball.position));
  }

  private static Path planDefense(DataPacket input, PredictionSlice predictionSlice) {
    BallData ballLocation = BallData.fromPredictionSlice(predictionSlice);

    // TODO: Fix this.
    CarData optimalCar = CarBallOptimizer.getOptimalApproach(ballLocation, Goal.opponentGoal(input.car.team).center);

    return planPath(input, optimalCar);
  }

  private static Path planShotOnGoal(DataPacket input, PredictionSlice predictionSlice) {
    BallData ballLocation = BallData.fromPredictionSlice(predictionSlice);

    CarData optimalCar = CarBallOptimizer.getOptimalApproach(ballLocation, Goal.opponentGoal(input.car.team).center);

    return planPath(input, optimalCar);
  }

  // TODO: Add in U-turn / drift.
  public static Path planPath(DataPacket input, CarData targetCarData) {
    CarData projectedCardData = CarDataUtils.rewind(targetCarData, 200);
    Path.Segment tail = new Path.Segment(projectedCardData.position, targetCarData.position);

    Circle approachCircle = Paths.closeApproach(projectedCardData, input.car);

    boolean insideApproachCircle = input.car.position.distance(approachCircle.center) < approachCircle.radius;

    boolean isTravelingTowardGoal = Math.signum(input.car.velocity.dot(projectedCardData.velocity)) > 0;

    if (isTravelingTowardGoal && insideApproachCircle) {
      return new Path(input.car, targetCarData, new Path.Segment(input.car.position, targetCarData, approachCircle));
    }

    Vector3 tangentPoint = Paths.tangent(approachCircle, input.car.position, projectedCardData);

    Path.Segment arcSegment = new Path.Segment(
        tangentPoint, projectedCardData, approachCircle);

    Path.Segment lineSegment = new Path.Segment(input.car.position, tangentPoint);

    return new Path(input.car, targetCarData, lineSegment, arcSegment, tail);
  }
}
