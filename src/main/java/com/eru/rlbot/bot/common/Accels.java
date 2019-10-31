package com.eru.rlbot.bot.common;

import java.util.Optional;

public class Accels {

  private static final double SLOW_SLOPE = (1600.0 - 160) / (0 - 1400);
  private static final double FAST_SLOPE = (160.0 - 0) / (1400 - 1410);

  public static float acceleration(double carVelocity) {
    if (carVelocity < 1400) {
      return (float) (1600 + (SLOW_SLOPE * carVelocity));
    } else if (carVelocity < 1410) {
      return (float) (160 + (FAST_SLOPE * (carVelocity - 1400)));
    } else {
      // Cannot throttle faster.
      return 0;
    }
  }

  private static final double STEP_SIZE = 1.0/120;
  public static float timeToDistance(double velocity, double distance) {
    float t = 0;
    while (distance > 0) {
      float nextAcceleration = acceleration(velocity);
      double newVelocity = velocity + nextAcceleration * STEP_SIZE;

      distance -= ((velocity + newVelocity) / 2) * STEP_SIZE;
      velocity = newVelocity;
      t += STEP_SIZE;
    }
    return t;
  }

  public static float boostedTimeToDistance(double velocity, double distance) {
    float t = 0;
    while (distance > 0) {
      double nextAcceleration = acceleration(velocity) + Constants.BOOSTED_ACCELERATION;
      double newVelocity = velocity + nextAcceleration * STEP_SIZE;

      distance -= ((velocity + newVelocity) / 2) * STEP_SIZE;
      velocity = newVelocity;
      t += STEP_SIZE;
    }
    return t;
  }

  public static float jumpTimeToHeight(double distance) {
    double t = 0;
    double velocity = Constants.JUMP_VELOCITY_INSTANT;
    while (distance > 0) {
      double nextAcceleration = jumpAcceleration(t);
      double newVelocity = velocity + nextAcceleration * STEP_SIZE;

      distance -= ((velocity + newVelocity) / 2) * STEP_SIZE;
      velocity = newVelocity;
      t += STEP_SIZE;
    }
    return (float) t;
  }

  public static Optional<Float> floatingTimeToTarget(double zVelocity, double zDistance) {
    double t = 0;
    while (zDistance > 0) {
      double newVelocity = zVelocity + Constants.NEG_GRAVITY * STEP_SIZE;

      zDistance -= ((zVelocity + newVelocity) / 2) * STEP_SIZE;

      if (zDistance > 0 && newVelocity < 0) {
        // Need a second jump
        return Optional.empty();
      }

      zVelocity = newVelocity;
      t += STEP_SIZE;
    }
    return Optional.of((float) t);
  }

  // TODO: Assumes car is flat on the ground.
  private static double jumpAcceleration(double time) {
    if (time > Constants.JUMP_HOLD_TIME) {
      return 0;
    } else {
      return Constants.JUMP_ACCELERATION_HELD + Constants.NEG_GRAVITY;
    }
  }
}
