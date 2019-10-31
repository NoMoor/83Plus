package com.eru.rlbot.common.jump;

import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

public class JumpManager {

  private static final float MAX_JUMP_TIME = .2f;

  private static final int JUMP_RELEASE_COUNT = 90;

  private static boolean jumpPressed;
  private static float firstJumpTime;
  private static float secondJumpTime;
  private static boolean canFlip;
  private static int jumpInAirReleased;

  // Updated each cycle
  private static DataPacket input;

  public static void loadDataPacket(DataPacket dataPacket) {
    input = dataPacket;

    if (!input.car.hasWheelContact && !jumpPressed) {
      // We've been bumped and we can flip whenever.
      canFlip = true;
      jumpInAirReleased++;
    } else if (input.car.hasWheelContact) {
      firstJumpTime = 0;
      secondJumpTime = 0;
      jumpInAirReleased = 0;
    }
  }

  public static void processOutput(ControlsOutput output, DataPacket input) {
    if (hasMaxJumpHeight()) {
      // Automatically let go of the jump button.
      output.withJump(false);
    }

    jumpPressed = output.holdJump();

    if (!input.car.hasWheelContact && jumpPressed && firstJumpTime == 0) {
      firstJumpTime = input.car.elapsedSeconds;
      canFlip = false;
    } else if (!input.car.hasWheelContact && !jumpPressed && secondJumpTime != 0) {
      jumpInAirReleased++;
    }

    if (!input.car.hasWheelContact && jumpPressed && hasReleasedJumpInAir()) {
      // Dodging now.
      canFlip = false;
      secondJumpTime = input.car.elapsedSeconds;
    } else if (firstJumpTime > 0 && secondJumpTime == 0 && hasReleasedJumpInAir()) {
      canFlip = true;
    }
  }

  // Return false if we were bumped instead of jumping.
  public static boolean hasMaxJumpHeight() {
    return elapsedJumpTime() > MAX_JUMP_TIME;
  }

  public static float elapsedJumpTime() {
    return firstJumpTime == 0 ? 0 : input.car.elapsedSeconds - firstJumpTime;
  }

  public static boolean canFlip() {
    return canFlip;
  }

  public static boolean hasReleasedJumpInAir() {
    return jumpInAirReleased > JUMP_RELEASE_COUNT;
  }

  public static int getJumpCount() {
    return jumpInAirReleased;
  }
}
