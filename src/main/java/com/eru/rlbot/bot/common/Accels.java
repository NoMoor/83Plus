package com.eru.rlbot.bot.common;

import static com.eru.rlbot.bot.common.Constants.STEP_SIZE;

import com.eru.rlbot.common.Numbers;
import com.eru.rlbot.common.Pair;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.vector.Vector3;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utilities to determine acceleration and travel times and related constants.
 */
public class Accels {

  private static final float SLOW_SLOPE = (1600.0f - 160) / (0 - 1400);
  private static final float FAST_SLOPE = (160.0f - 0) / (1400 - 1410);

  /** Returns the acceleration of the next frame given a throttle of 1.0. */
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

  /**
   * Returns the minimum time to go the distance by using all the boost available.
   */
  public static AccelResult minTimeToDistance(CarData car, double distance) {
    double velocity = car.velocity.flatten().magnitude();

    return boostedTimeToDistance(car.boost, velocity, distance);
  }

  /**
   * Returns the time to travel a distance without using boost.
   */
  public static AccelResult nonBoostedTimeToDistance(double velocity, double distance) {
    double initialDistance = distance;
    float t = 0;
    while (distance > 0) {
      float nextAcceleration = acceleration(velocity);
      double newVelocity = velocity + nextAcceleration * STEP_SIZE;

      distance -= ((velocity + newVelocity) / 2) * STEP_SIZE;
      velocity = newVelocity;
      t += STEP_SIZE;
    }

    if (distance < 0) {
      t -= STEP_SIZE;
    }

    return new AccelResult(velocity, t, initialDistance, 0);
  }

  /** Returns the time to travel the given distance using unlimited boost. */
  public static AccelResult boostedTimeToDistance(double velocity, double distance) {
    double boostUsed = 0;
    double initialDistance = distance;
    float t = 0;
    while (distance > 0) {
      double nextAcceleration = acceleration(velocity) + Constants.BOOSTED_ACCELERATION;
      double newVelocity =
          Numbers.clamp(velocity + nextAcceleration * STEP_SIZE, 0, Constants.BOOSTED_MAX_SPEED);

      distance -= ((velocity + newVelocity) / 2) * STEP_SIZE;
      velocity = newVelocity;
      t += STEP_SIZE;
      boostUsed += Constants.BOOST_RATE / STEP_SIZE;
    }
    return new AccelResult(velocity, t, initialDistance, boostUsed);
  }

  /** Returns the time to travel the given distance using unlimited boost. */
  public static AccelResult boostedTimeToDistance(double boost, double velocity, double distance) {
    double initialDistance = distance;
    double initialBoost = boost;

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
    return new AccelResult(velocity, t, initialDistance, initialBoost - boost);
  }

  /** The amount of time needed to get to a given height or Optional.empty. */
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
      int jumpTick = (int) (time / STEP_SIZE);
      return (jumpTick < Constants.JUMP_ACCELERATION_HOLD_SLOW_TICK_COUNT
          ? Constants.JUMP_ACCELERATION_SLOW_HELD
          : Constants.JUMP_ACCELERATION_FAST_HELD)
          + Constants.NEG_GRAVITY;
    }
  }

  public static final ConcurrentHashMap<Pair<Integer, Integer>, AccelResult> slowingDistances
      = new ConcurrentHashMap<>();

  public static AccelResult distanceToSlow(double currentVelocity, double finalVelocity) {
    Pair<Integer, Integer> key = Pair.of(fuzz(currentVelocity), fuzz(finalVelocity));

    return slowingDistances.computeIfAbsent(key, Accels::computeSlowingDistance);
  }

  private static int fuzz(double value) {
    return (int) ((value + 5) / 10) * 10;
  }

  private static AccelResult computeSlowingDistance(Pair<Integer, Integer> velocityPair) {
    double currentVelocity = velocityPair.getFirst();
    double finalVelocity = velocityPair.getSecond();

    double time = 0;
    double d = 0;
    while (currentVelocity > finalVelocity) {
      d += currentVelocity * STEP_SIZE;
      currentVelocity -= Constants.BREAKING_DECELERATION * STEP_SIZE;
      time += STEP_SIZE;
    }
    return new AccelResult(finalVelocity, time, d, 0);
  }

  public static AccelResult accelerateForTime(double initialVelocity, double initialTime, double initialBoost) {
    double boostRemaining = initialBoost;
    double distanceTraveled = 0;
    double timeRemaining = initialTime;
    double currentVelocity = initialVelocity;

    while (timeRemaining > 0) {
      double nextAcceleration = acceleration(currentVelocity)
          + ((boostRemaining > 0) ? Constants.BOOSTED_ACCELERATION : 0);
      double newVelocity = currentVelocity + nextAcceleration * STEP_SIZE;
      distanceTraveled += ((currentVelocity + newVelocity) / 2) * STEP_SIZE;
      currentVelocity = newVelocity;
      timeRemaining -= STEP_SIZE;

      if (boostRemaining > 0) {
        boostRemaining -= STEP_SIZE * Constants.BOOST_RATE;
      }
    }
    return new AccelResult(currentVelocity, initialTime, distanceTraveled, initialBoost - boostRemaining);
  }

  // TODO: Change to AccelResult.
  public static double verticalDistance(float initialVerticalSpeed, double travelTime) {
    double time = travelTime;
    double distance = 0;
    double speed = initialVerticalSpeed;

    while (time > 0) {
      speed += Constants.NEG_GRAVITY * STEP_SIZE;
      distance += speed * STEP_SIZE;
      time -= STEP_SIZE;
    }
    return distance;
  }

  /**
   * The result of an acceleration simulation.
   */
  public static class AccelResult {
    public final double distance;
    public final double speed;
    public final double time;
    public final double boost;

    private AccelResult(double speed, double time, double distance, double boost) {
      this.speed = speed;
      this.time = time;
      this.distance = distance;
      this.boost = boost;
    }
  }

  /** Returns the impulse vector given a flip and the initial conditions. */
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
      sideImpulse = orientation.getNoseVector().flat().counterClockwisePerpendicular()
          .toMagnitude(sideImpulseMagnitude * Numbers.clamp(yaw + roll, -1, 1));
    }

    return frontImpulse.plus(sideImpulse);
  }

  /** Returns the angular acceleration vector for the given pitch, yaw, roll and orientation. */
  public static Vector3 flipAngularAcceleration(
      Orientation orientation, double pitch, double yaw, double roll) {

    double yawRoll = Numbers.clamp(yaw + roll, -1, 1);
    double total = Math.abs(pitch) + Math.abs(yawRoll);

    Vector3 side = orientation.getNoseVector().toMagnitude(yawRoll / total);
    Vector3 front = orientation.getRightVector().toMagnitude(-pitch / total);

    return side.plus(front)
        .multiply(Constants.STEP_SIZE_COUNT * Constants.MAX_ANGULAR_VELOCITY / JumpManager.FLIP_ACCELERATION_TICKS);
  }
}
