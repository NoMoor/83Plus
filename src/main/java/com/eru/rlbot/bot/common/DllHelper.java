package com.eru.rlbot.bot.common;

import rlbot.cppinterop.RLBotDll;
import rlbot.cppinterop.RLBotInterfaceException;
import rlbot.flat.BallPrediction;
import rlbot.flat.FieldInfo;

import java.util.Optional;

public class DllHelper {

  public static Optional<BallPrediction> getBallPrediction() {
    try {
      return Optional.of(RLBotDll.getBallPrediction());
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
