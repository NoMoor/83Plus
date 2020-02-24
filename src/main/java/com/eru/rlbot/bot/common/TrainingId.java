package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;

public class TrainingId {

  private static CarData previousCar;

  private static long id = System.currentTimeMillis();

  public static void track(DataPacket input) {
    // Only track one car at a time. Both can share the same training id.
    if (previousCar != null && previousCar.playerIndex != input.car.playerIndex) {
      return;
    }

    if (previousCar != null && previousCar.position.distance(input.car.position) > 200) {
      id++;
    }
    previousCar = input.car;
  }

  public static long getId() {
    return id;
  }
}
