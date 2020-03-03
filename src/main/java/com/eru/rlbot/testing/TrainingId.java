package com.eru.rlbot.testing;

import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;

/**
 * Creates a training id for use in logging.
 */
public final class TrainingId {

  private static CarData previousCar;

  private static long id = System.currentTimeMillis();

  /** Updates the training id when the car or ball are teleported. */
  public static void track(DataPacket input) {
    // Only track one car at a time. Both can share the same training id.
    if (previousCar != null && previousCar.serialNumber != input.car.serialNumber) {
      return;
    }

    if (previousCar != null && previousCar.position.distance(input.car.position) > 200) {
      id++;
    }
    previousCar = input.car;
  }

  /**
   * Returns the current id.
   */
  public static long getId() {
    return id;
  }

  private TrainingId() {}
}
