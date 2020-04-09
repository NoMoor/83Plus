package com.eru.rlbot.common.jump;

import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.Controls;

/**
 * Keeps track of the various aspects of the jump state of a car.
 */
public class JumpManager {

  public static final float MAX_JUMP_TIME = .2f;

  private static final int JUMP_RELEASE_COUNT = 1;

  public static final float FLIP_ACCELERATION_TICKS = 3;
  public static final float FLIP_I_TIME = .15f;
  public static final float FLIP_ACCELERATION_TIME = FLIP_ACCELERATION_TICKS / Constants.STEP_SIZE_COUNT;
  public static final float FLIP_NON_CANCELLABLE_TIME = 5f / Constants.STEP_SIZE_COUNT;
  public static final float FLIP_TIME = .65f;
  public static final float FLIP_UPWARD_Z_DULL_TIME = .075f;
  public static final float FLIP_Z_DULL_FACTOR = .65f;
  public static final float FLIP_Z_FALL = -15; // uu/s
  public static final float GROUND_CONTACT_TIME = 3f / Constants.STEP_SIZE_COUNT;

  private static ThreadLocal<JumpManager> INSTANCE = ThreadLocal.withInitial(JumpManager::new);

  private boolean jumpPressed;
  private float firstJumpTime;
  private float secondJumpTime;
  private boolean canFlip;
  private int jumpInAirReleased;

  private float startFlipTime;
  private float flipPitch;
  private float flipYaw;
  private float flipRoll;

  // Updated each cycle
  private CarData inputCar;

  public static JumpManager forCar(CarData car) {
    if (!car.isLiveData) {
      throw new IllegalStateException("Not supported for non-live data");
    }

    return get(car);
  }

  private static JumpManager get(CarData car) {
    // TODO: Does this work?
    return INSTANCE.get();
  }

  public static JumpManager copyForCar(CarData car) {
    JumpManager copy = get(car).copy();
    copy.trackInput(car);
    return copy;
  }

  private JumpManager copy() {
    JumpManager copy = new JumpManager();
    copy.jumpPressed = this.jumpPressed;
    copy.firstJumpTime = this.firstJumpTime;
    copy.secondJumpTime = this.secondJumpTime;
    copy.canFlip = this.canFlip;
    copy.jumpInAirReleased = this.jumpInAirReleased;
    copy.startFlipTime = this.startFlipTime;
    copy.flipPitch = this.flipPitch;
    copy.flipYaw = this.flipYaw;
    copy.flipRoll = this.flipRoll;
    return copy;
  }

  public static void trackInput(DataPacket input) {
    forCar(input.car).trackInput(input.car);
  }

  public void trackInput(CarData car) {
    // TODO: Update to use single / double jump bits...
    inputCar = car;

    if (!car.hasWheelContact && !jumpPressed) {
      jumpInAirReleased++;
      // We've been bumped and we can flip whenever.
      if (hasReleasedJumpInAir() && secondJumpTime == 0) {
        canFlip = car.position.z > 30;
      }
    } else if (car.hasWheelContact && !car.jumped && !jumpPressedLastFrame()) {
      firstJumpTime = 0;
      secondJumpTime = 0;
      jumpInAirReleased = 0;
      startFlipTime = 0;
      flipRoll = 0;
      flipYaw = 0;
      flipPitch = 0;
      canFlip = false;
    }
  }

  public static void trackOutput(DataPacket input, Controls output) {
    forCar(input.car).trackOutput(input.car, output);
  }

  public void trackOutput(CarData car, Controls output) {
    jumpPressed = output.holdJump();

    if (car.hasWheelContact && jumpPressed && firstJumpTime == 0) {
      // We will jump on this frame.
      firstJumpTime = car.elapsedSeconds;
      canFlip = false;
    } else if (!car.hasWheelContact && !jumpPressed && firstJumpTime != 0) {
      jumpInAirReleased++;
    }

    if (firstJumpTime > 0 && jumpPressed && canFlip) {
      // Dodging now.
      canFlip = false;
      secondJumpTime = car.elapsedSeconds;
      if (output.getYaw() != 0 || output.getRoll() != 0 || output.getPitch() != 0) {
        startFlipTime = car.elapsedSeconds;
        flipPitch = output.getPitch();
        flipYaw = output.getYaw();
        flipRoll = output.getRoll();
      }
    } else if (firstJumpTime > 0 && secondJumpTime == 0 && hasReleasedJumpInAir()) {
      canFlip = car.position.z > 30;
    }
  }

  // Return false if we were bumped instead of jumping.
  public boolean hasMaxJumpHeight() {
    return getElapsedJumpTime() >= MAX_JUMP_TIME;
  }

  public float getElapsedJumpTime() {
    return firstJumpTime == 0 ? 0 : inputCar.elapsedSeconds - firstJumpTime;
  }

  public boolean canFlip() {
    return canFlip;
  }

  public boolean hasReleasedJumpInAir() {
    return jumpInAirReleased >= JUMP_RELEASE_COUNT;
  }

  public boolean jumpPressedLastFrame() {
    return jumpPressed;
  }

  public boolean canJump() {
    return firstJumpTime == 0 && !canFlip;
  }

  public float getFlipTime() {
    return startFlipTime;
  }

  public boolean isFlipping() {
    return startFlipTime != 0 && inputCar.elapsedSeconds - startFlipTime < FLIP_TIME;
  }

  public float getFlipPitch() {
    return flipPitch;
  }

  public float getFlipYaw() {
    return flipYaw;
  }

  public float getFlipRoll() {
    return flipRoll;
  }

  public boolean canJumpAccelerate() {
    return jumpPressedLastFrame() && !hasMaxJumpHeight();
  }
}
