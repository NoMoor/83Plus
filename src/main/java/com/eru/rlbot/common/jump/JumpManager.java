package com.eru.rlbot.common.jump;

import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

public class JumpManager {

  private static boolean jumpPressed;
  private static float jumpTime;
  private static boolean canDodge;
  private static boolean jumpInAirReleased;

  // Updated each cycle
  private static DataPacket input;

  public static void loadDataPacket(DataPacket dataPacket) {
    input = dataPacket;
  }

  public static void processOutput(ControlsOutput output, DataPacket input) {
    jumpPressed = output.holdJump();

    if (!input.car.hasWheelContact) {
      // TODO(ahatfield): This ignores ceiling shots and bumps.
      jumpTime = input.car.elapsedSeconds;
      canDodge = false;
    }

    jumpInAirReleased = !input.car.hasWheelContact && !jumpPressed;

    if (!input.car.hasWheelContact && jumpInAirReleased && output.holdJump()) {
      // Dodging now.
      canDodge = false;
    } else if (jumpTime > 0 && jumpInAirReleased) {
      canDodge = true;
    }
  }

  public static boolean hasMaxJumpHight() {
    return input.car.elapsedSeconds - jumpTime > .75;
  }

  public static boolean canDodge() {
    return false;
  }
}
