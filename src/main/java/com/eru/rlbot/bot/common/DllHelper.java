package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.vector.Vector3;
import rlbot.cppinterop.RLBotDll;
import rlbot.cppinterop.RLBotInterfaceException;
import rlbot.flat.BallPrediction;
import rlbot.flat.FieldInfo;

import java.util.Optional;

public class DllHelper {

  public static Optional<BallPrediction> getBallPrediction() {
    try {
      BallPrediction ballPrediction = RLBotDll.getBallPrediction();

      // TODO: Make this cleaner.
      // If the velocity is 0, pretend we dont' have ball prediction.
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
}
