package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.input.CarData;

public class TrainingId {

  private static CarData previousCar;

  private static long id = System.currentTimeMillis();

  public static void trackId(CarData car) {
    if (previousCar != null && previousCar.position.distance(car.position) > 200) {
      id++;
    }
    previousCar = car;
  }

  public static long getId() {
    return id;
  }
}
