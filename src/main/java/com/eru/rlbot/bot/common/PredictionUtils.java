package com.eru.rlbot.bot.common;

import com.eru.rlbot.bot.prediction.BallPredictionUtil;
import com.eru.rlbot.common.DllHelper;
import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;
import java.util.Optional;
import rlbot.flat.BallPrediction;
import rlbot.flat.PredictionSlice;

/**
 * Utility for tracking ball predictions.
 * <p>
 * DEPRECATED: Use {@link BallPredictionUtil}
 */
@Deprecated
public final class PredictionUtils {

  public static Moment getFirstHittableBall(DataPacket input) {
    Optional<BallPrediction> ballPredictionOptional = DllHelper.getBallPrediction();

    if (!ballPredictionOptional.isPresent()) {
      return Moment.from(input.ball);
    }

    BallPrediction ballPrediction = ballPredictionOptional.get();
    for (int i = 0; i < ballPrediction.slicesLength(); i++) {
      PredictionSlice predictionSlice = ballPrediction.slices(i);

      Vector3 ballLocation = Vector3.of(predictionSlice.physics().location());

      if (ballLocation.z > 200) {
        continue;
      }

      double distance = ballLocation.flatten().distance(input.car.position.flatten());

      double timeToBall = Accels.minTimeToDistance(input.car, distance).getTime();
      float timeToPrediction = predictionSlice.gameSeconds() - input.car.elapsedSeconds;

      if (timeToBall < timeToPrediction) {
        return new Moment(predictionSlice);
      }
    }

    return Moment.from(input.ball);
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
