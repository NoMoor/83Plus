package com.eru.rlbot.common;

import com.eru.rlbot.common.input.BallData;
import java.util.Optional;
import rlbot.cppinterop.RLBotDll;
import rlbot.cppinterop.RLBotInterfaceException;
import rlbot.flat.BallPrediction;
import rlbot.flat.FieldInfo;
import rlbot.flat.PredictionSlice;

public class DllHelper {

  public static Optional<BallPrediction> getBallPrediction() {
    try {
      BallPrediction ballPrediction = RLBotDll.getBallPrediction();

      // TODO: Make this cleaner.
      // TODO: Wrap ball ball to have iterator and Vector types or at least easier operation.
      // If the velocity is 0, pretend we don't have ball ball.
      // Vector3 ballVelocity = Vector3.of(ballPrediction.slices(0).physics().velocity());

      return Optional.of(ballPrediction);
    } catch (RLBotInterfaceException e) {
      // e.printStackTrace(); Somewhat expected
      return Optional.empty();
    }
  }

  public static Optional<FieldInfo> getFieldInfo() {
    try {
      return Optional.of(RLBotDll.getFieldInfo());
    } catch (RLBotInterfaceException e) {
      // e.printStackTrace(); Somewhat expected
      return Optional.empty();
    }
  }

  private DllHelper() {}

  public static BallData getPredictedBallAtTime(BallData ball, float gameTime) {
    Optional<BallPrediction> predictionOptional = getBallPrediction();
    if (!predictionOptional.isPresent()) {
      return ball;
    }

    // TODO: Update this to binary search.
    BallPrediction ballPrediction = predictionOptional.get();
    for (int i = 0 ; i < ballPrediction.slicesLength() ; i++) {
      PredictionSlice predictionSlice = ballPrediction.slices(i);
      if (predictionSlice.gameSeconds() > gameTime) {
        return BallData.fromPredictionSlice(predictionSlice);
      }
    }
    return BallData.fromPredictionSlice(ballPrediction.slices(ballPrediction.slicesLength() - 1));
  }
}
