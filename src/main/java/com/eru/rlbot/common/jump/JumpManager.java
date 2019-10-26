package com.eru.rlbot.common.jump;

import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

public class JumpManager {

  private static final float MAX_JUMP_TIME = .75f;

  private static boolean jumpPressed;
  private static float jumpTime;
  private static boolean canFlip;
  private static boolean jumpInAirReleased;

  // Updated each cycle
  private static DataPacket input;

  public static void loadDataPacket(DataPacket dataPacket) {
    input = dataPacket;

    if (!input.car.hasWheelContact && !jumpPressed) {
      // We've been bumped and we can flip whenever.
      canFlip = true;
      jumpInAirReleased = true;
    } else if (input.car.hasWheelContact) {
      jumpTime = 0;
      jumpInAirReleased = false;
    }
  }

  public static void processOutput(ControlsOutput output, DataPacket input) {
    if (hasMaxJumpHeight()) {
      // Automatically let go of the jump button.
      output.withJump(false);
    }

    jumpPressed = output.holdJump();

    if (!input.car.hasWheelContact && jumpPressed) {
      jumpTime = input.car.elapsedSeconds;
      canFlip = false;
    } else if (!input.car.hasWheelContact && !jumpPressed) {
      jumpInAirReleased = true;
    }

    if (!input.car.hasWheelContact && jumpInAirReleased && jumpPressed) {
      // Dodging now.
      canFlip = false;
    } else if (jumpTime > 0 && jumpInAirReleased) {
      canFlip = true;
    }
  }

  // Return false if we were bumped instead of jumping.
  public static boolean hasMaxJumpHeight() {
    return elapsedJumpTime() > MAX_JUMP_TIME;
  }

  private static float elapsedJumpTime() {
    return jumpTime == 0 ? 0 : input.car.elapsedSeconds - jumpTime;
  }

  public static boolean canFlip() {
    return canFlip;
  }

  public static boolean hasJumpReleasedInAir() {
    return jumpInAirReleased;
  }
}
