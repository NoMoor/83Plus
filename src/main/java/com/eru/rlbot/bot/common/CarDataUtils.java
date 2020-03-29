package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.vector.Vector3;

/**
 * Utilities for operating on car data.
 */
public final class CarDataUtils {

  /** Moves the car back a specified distance based on the current velocity. */
  public static CarData rewindDistance(CarData car, double distance) {
    double time = distance / car.velocity.magnitude();

    return rewindTime(car, time);
  }

  /** Moves the car back a specified time based on the current velocity. */
  public static CarData rewindTime(CarData car, double time) {
    Vector3 velocity = car.velocity;
    if (Math.abs(velocity.z) < 10) {
      velocity = velocity.setZ(0);
    }

    return car.toBuilder()
        .setPosition(car.position.minus(velocity.multiply(time)))
        .setTime(car.elapsedSeconds - time)
        .build();
  }

  private CarDataUtils() {}
}
