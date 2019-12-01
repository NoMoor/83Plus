package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.flat.BallPrediction;
import rlbot.flat.PredictionSlice;
import java.util.Optional;

public class PredictionUtils {

  public static Moment getFirstHittableBall(DataPacket input) {
    Optional<BallPrediction> ballPredictionOptional = DllHelper.getBallPrediction();

    if (!ballPredictionOptional.isPresent()) {
      // TODO: This should likely include time.
      return new Moment(input.ball);
    }

    BallPrediction ballPrediction = ballPredictionOptional.get();
    for (int i = 0 ; i < ballPrediction.slicesLength() ; i++) {
      PredictionSlice predictionSlice = ballPrediction.slices(i);

      Vector3 ballLocation = Vector3.of(predictionSlice.physics().location());

      if (ballLocation.z > 200) {
        continue;
      }

      double distance = ballLocation.flatten().distance(input.car.position.flatten());

      float timeToBall = Accels.minTimeToDistance(input.car, distance);
      float timeToPrediction = predictionSlice.gameSeconds() - input.car.elapsedSeconds;

      if (timeToBall < timeToPrediction) {
        return new Moment(predictionSlice);
      }
    }

    return new Moment(input.ball, input.car.elapsedSeconds);
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
