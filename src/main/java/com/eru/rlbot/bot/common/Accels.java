package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.vector.Vector3;
import java.util.Optional;

public class Accels {

  private static final float SLOW_SLOPE = (1600.0f - 160) / (0 - 1400);
  private static final float FAST_SLOPE = (160.0f - 0) / (1400 - 1410);

  public static final double MAX_ACCELERATION = acceleration(0) + Constants.BOOSTED_ACCELERATION;

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
      int jumpTick = (int) (time / Constants.STEP_SIZE);
      return (jumpTick < Constants.JUMP_ACCELERATION_HOLD_SLOW_TICK_COUNT
          ? Constants.JUMP_ACCELERATION_SLOW_HELD
          : Constants.JUMP_ACCELERATION_FAST_HELD)
          + Constants.NEG_GRAVITY;
    }
  }

  public static double distanceToSlow(double currentVelocity, double finalVelocity) {
    double d = 0;
    while (currentVelocity > finalVelocity) {
      d += currentVelocity * STEP_SIZE;
      currentVelocity -= Constants.BREAKING_DECELERATION * STEP_SIZE;
    }
    return d;
  }

  public static class AccelResult {
    public final double speed;
    public final double time;

    private AccelResult(double speed, double time) {
      this.speed = speed;
      this.time = time;
    }
  }

  public static Vector3 flipImpulse(Orientation orientation, Vector3 velocity, double pitch, double yaw, double roll) {
    Vector3 frontImpulse = Vector3.zero();
    Vector3 sideImpulse = Vector3.zero();

    if (pitch != 0) {
      if (pitch > 0) {
        // backflip
        double vForward = orientation.getNoseVector().dot(velocity);
        double backImpulse = -Constants.FORWARD_DODGE_IMPULSE * (1 + .5 * (vForward / Constants.BOOSTED_MAX_SPEED));
        frontImpulse = orientation.getNoseVector().flat().toMagnitude(backImpulse);
      } else {
        frontImpulse = orientation.getNoseVector().flat().toMagnitude(Constants.FORWARD_DODGE_IMPULSE);
      }
    }

    if (yaw != 0 || roll != 0) {
      double vForward = orientation.getNoseVector().dot(velocity);
      double sideImpulseMagnitude = Constants.FORWARD_DODGE_IMPULSE * (1 + .9 * (vForward / Constants.BOOSTED_MAX_SPEED));
      sideImpulse = orientation.getNoseVector().flat().clockwisePerpendicular()
          .toMagnitude(sideImpulseMagnitude * Angles3.clip(yaw + roll, -1, 1));
    }

    return frontImpulse.plus(sideImpulse);
  }

  public static Vector3 flipAngularAcceleration(
      Orientation orientation, double pitch, double yaw, double roll) {

    double yawRoll = Angles3.clip(yaw + roll, -1, 1);
    double total = Math.abs(pitch) + Math.abs(yawRoll);

    Vector3 side = orientation.getNoseVector().toMagnitude(yawRoll / total);
    Vector3 front = orientation.getLeftVector().toMagnitude(-pitch / total);

    return side.plus(front)
        .multiply(Constants.STEP_SIZE_COUNT * Constants.MAX_ANGULAR_VELOCITY / JumpManager.FLIP_ACCELERATION_TICKS);
  }
}
