package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.flat.BallPrediction;
import rlbot.flat.PredictionSlice;
import java.util.Optional;

public class PredictionUtils {

  public static Moment getFirstHittableBall(CarData car, BallData ball) {
    Optional<BallPrediction> ballPredictionOptional = DllHelper.getBallPrediction();

    if (!ballPredictionOptional.isPresent()) {
      // TODO: This should likely include time.
      return new Moment(ball);
    }

    BallPrediction ballPrediction = ballPredictionOptional.get();
    for (int i = 0 ; i < ballPrediction.slicesLength() ; i++) {
      PredictionSlice predictionSlice = ballPrediction.slices(i);

      double distance = Vector3.of(predictionSlice.physics().location()).distance(car.position);

      float timeToBall = Accels.minTimeToDistance(car, distance);
      float timeToPrediction = predictionSlice.gameSeconds() - car.elapsedSeconds;

      if (timeToBall < timeToPrediction) {
        return new Moment(predictionSlice);
      }
    }

    return new Moment(ball, car.elapsedSeconds);
  }

  public static Optional<PredictionSlice> getBallInGoalSlice() {
    Optional<BallPrediction> ballPredictionOptional = DllHelper.getBallPrediction();
    if (!ballPredictionOptional.isPresent()) {
      return Optional.empty();
    }

    BallPrediction ballPrediction = ballPredictionOptional.get();
    for (int i = 0 ; i < ballPrediction.slicesLength() ; i++) {
      PredictionSlice predictionSlice = ballPrediction.slices(i);
      if (Math.abs(predictionSlice.physics().location().y()) > Constants.HALF_LENGTH + Constants.BALL_RADIUS) {
        return Optional.of(predictionSlice);
      }
    }

    return Optional.empty();
  }
}
