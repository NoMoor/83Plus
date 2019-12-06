package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.input.CarData;
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

  public static AccelResult minTimeToDistance(CarData car, double distance) {
    double velocity = car.velocity.flatten().norm();

    return boostedTimeToDistance(car.boost, velocity, distance);
  }

  public static AccelResult minTimeToDistance(CarData car, double distance, double targetSpeed) {
    double velocity = car.velocity.flatten().norm();

    return boostedTimeToDistance(car.boost, velocity, targetSpeed, distance);
  }

  private static final double STEP_SIZE = 1.0/120;
  public static AccelResult timeToDistance(double velocity, double distance) {
    float t = 0;
    while (distance > 0) {
      float nextAcceleration = acceleration(velocity);
      double newVelocity = velocity + nextAcceleration * STEP_SIZE;

      distance -= ((velocity + newVelocity) / 2) * STEP_SIZE;
      velocity = newVelocity;
      t += STEP_SIZE;
    }
    return new AccelResult(velocity, t);
  }

  public static AccelResult boostedTimeToDistance(double velocity, double distance) {
    float t = 0;
    while (distance > 0) {
      double nextAcceleration = acceleration(velocity) + Constants.BOOSTED_ACCELERATION;
      double newVelocity = velocity + nextAcceleration * STEP_SIZE;

      distance -= ((velocity + newVelocity) / 2) * STEP_SIZE;
      velocity = newVelocity;
      t += STEP_SIZE;
    }
    return new AccelResult(velocity, t);
  }

  public static AccelResult boostedTimeToDistance(double boost, double velocity, double distance) {
    float t = 0;
    while (distance > 0) {
      double nextAcceleration = acceleration(velocity) + ((boost > 0) ? Constants.BOOSTED_ACCELERATION : 0);
      double newVelocity = velocity + nextAcceleration * STEP_SIZE;

      distance -= ((velocity + newVelocity) / 2) * STEP_SIZE;
      velocity = newVelocity;
      t += STEP_SIZE;

      if (boost > 0) {
        boost -= STEP_SIZE * 33;
      }
    }
    return new AccelResult(velocity, t);
  }

  // TODO: Make more sophisticated. For now. Assume the we wont' exceed Max(velocity, targetVelocity)
  public static AccelResult boostedTimeToDistance(double boost, double velocity, double targetVelocity, double distance) {
    float t = 0;
    while (distance > 0) {
      double nextAcceleration = 0;
      if (targetVelocity > velocity) {
        nextAcceleration = acceleration(velocity) + ((boost > 0) ? Constants.BOOSTED_ACCELERATION : 0);
      }

      double newVelocity = velocity + nextAcceleration * STEP_SIZE;
      distance -= ((velocity + newVelocity) / 2) * STEP_SIZE;
      velocity = newVelocity;
      t += STEP_SIZE;

      if (boost > 0) {
        boost -= STEP_SIZE * 33;
      }
    }
    return new AccelResult(velocity, t);
  }

  public static Optional<Float> jumpTimeToHeight(double distance) {
    double t = 0;
    double velocity = Constants.JUMP_VELOCITY_INSTANT;

    boolean useDoubleJump = false;

    while (distance > 0) {
      double nextAcceleration = jumpAcceleration(t);
      double newVelocity = velocity + nextAcceleration * STEP_SIZE;

      if (t > .255 && !useDoubleJump) {
        newVelocity += Constants.JUMP_VELOCITY_INSTANT;
        useDoubleJump = true;
      }

      distance -= ((velocity + newVelocity) / 2) * STEP_SIZE;

      if (velocity < 0 && distance > 0) {
        return Optional.empty();
      }

      velocity = newVelocity;
      t += STEP_SIZE;
    }
    return Optional.of((float) t);
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
      return Constants.NEG_GRAVITY;
    } else {
      return Constants.JUMP_ACCELERATION_HELD + Constants.NEG_GRAVITY;
    }
  }

  public static double speedAt(double groundSpeed, double norm) {
    return 0;
  }

  public static class AccelResult {
    public final double speed;
    public final double time;

    private AccelResult(double speed, double time) {
      this.speed = speed;
      this.time = time;
    }
  }
}
