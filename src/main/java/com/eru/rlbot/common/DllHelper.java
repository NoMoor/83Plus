package com.eru.rlbot.common;

import com.eru.rlbot.common.input.BallData;
import java.util.Optional;
import rlbot.cppinterop.RLBotDll;
import rlbot.cppinterop.RLBotInterfaceException;
import rlbot.flat.BallPrediction;
import rlbot.flat.FieldInfo;
import rlbot.flat.PredictionSlice;

/**
 * Wrapper for the Dll to suppress any exceptions.
 */
public class DllHelper {

  public static Optional<BallPrediction> getBallPrediction() {
    try {
      return Optional.of(RLBotDll.getBallPrediction());
    } catch (RLBotInterfaceException e) {
      return Optional.empty();
    }
  }

  public static Optional<FieldInfo> getFieldInfo() {
    try {
      return Optional.of(RLBotDll.getFieldInfo());
    } catch (RLBotInterfaceException e) {
      return Optional.empty();
    }
  }

  public static BallData getPredictedBallAtTime(BallData ball, float gameTime) {
    Optional<BallPrediction> predictionOptional = getBallPrediction();
    if (!predictionOptional.isPresent()) {
      return ball;
    }

    // TODO: Update this to binary search.
    BallPrediction ballPrediction = predictionOptional.get();
    for (int i = 0; i < ballPrediction.slicesLength(); i++) {
      PredictionSlice predictionSlice = ballPrediction.slices(i);
      if (predictionSlice.gameSeconds() > gameTime) {
        return BallData.fromPredictionSlice(predictionSlice);
      }
    }
    return BallData.fromPredictionSlice(ballPrediction.slices(ballPrediction.slicesLength() - 1));
  }

  private DllHelper() {}
}
