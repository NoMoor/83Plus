package com.eru.rlbot.bot.common;

import static com.eru.rlbot.bot.common.Constants.NORMAL_EXPECTED;

import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;

public class DemoChecker {

  private static Vector3 lastBallPosition;
  private static Vector3 lastCarPosition;
  private static boolean reset;

  public static void track(DataPacket input) {
    boolean ballJumped = ballHasJumped(input);
    boolean carJumped = carHasJumped(input);
    reset = ballJumped || carJumped;
    updateCarAndBallTracking(input);
  }

  public static boolean wasDemoed() {
    return reset;
  }

  private static boolean carHasJumped(DataPacket input) {
    if (lastCarPosition == null) {
      return true;
    }

    return input.car.position.distance(lastCarPosition) > NORMAL_EXPECTED;
  }

  private static boolean ballHasJumped(DataPacket input) {
    if (lastBallPosition == null) {
      return true;
    }

    return input.ball.position.distance(lastBallPosition) > NORMAL_EXPECTED;
  }

  public static void updateCarAndBallTracking(DataPacket input) {
    lastCarPosition = input.car.position;
    lastBallPosition = input.ball.position;
  }
}
