package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.cppinterop.RLBotDll;
import rlbot.cppinterop.RLBotInterfaceException;
import rlbot.flat.BallPrediction;
import rlbot.flat.FieldInfo;
import rlbot.flat.PredictionSlice;

import java.util.Iterator;
import java.util.Optional;

public class DllHelper {

  public static Optional<BallPrediction> getBallPrediction() {
    try {
      BallPrediction ballPrediction = RLBotDll.getBallPrediction();

      // TODO: Make this cleaner.
      // TODO: Wrap ball prediction to have iterator and Vector types or at least easier operation.
      // If the velocity is 0, pretend we don't have ball prediction.
      Vector3 ballVelocity = Vector3.of(ballPrediction.slices(0).physics().velocity());

      return ballVelocity.norm() == 0 ? Optional.empty() : Optional.of(ballPrediction);
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

  public static Vector3 getProjectedBallPositionAtTime(BallData ball, float gameTime) {
    Optional<BallPrediction> predictionOptional = getBallPrediction();
    if (!predictionOptional.isPresent()) {
      return ball.position;
    }

    BallPrediction ballPrediction = predictionOptional.get();
    for (int i = 0 ; i < ballPrediction.slicesLength() ; i++) {
      PredictionSlice predictionSlice = ballPrediction.slices(i);
      if (predictionSlice.gameSeconds() > gameTime) {
        return Vector3.of(predictionSlice.physics().location());
      }
    }
    return Vector3.of(ballPrediction.slices(ballPrediction.slicesLength() - 1).physics().location());
  }
}
