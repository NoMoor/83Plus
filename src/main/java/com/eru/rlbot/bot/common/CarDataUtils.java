package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.input.CarData;

public final class CarDataUtils {

  public static CarData rewind(CarData carData, double distance) {
    double time = distance / carData.velocity.magnitude();

    return carData.toBuilder()
        .setPosition(carData.position.minus(carData.velocity.multiply(time)))
        .setTime(carData.elapsedSeconds - time)
        .build();
  }

  private CarDataUtils() {
  }
}
