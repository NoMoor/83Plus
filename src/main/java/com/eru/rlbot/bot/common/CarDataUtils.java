package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.input.CarData;

public final class CarDataUtils {

  public static CarData rewindDistance(CarData car, double distance) {
    double time = distance / car.velocity.magnitude();

    return rewindTime(car, time);
  }

  public static CarData rewindTime(CarData car, double time) {
    return car.toBuilder()
        .setPosition(car.position.minus(car.velocity.multiply(time)))
        .setTime(car.elapsedSeconds - time)
        .build();
  }

  private CarDataUtils() {
  }
}
