package com.eru.rlbot.common.boost;

import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.output.Controls;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the amount of boost and whether or not it will be active next frame.
 */
public class BoostTracker {

  private static ConcurrentHashMap<Integer, BoostTracker> MAP = new ConcurrentHashMap<>();

  /** Returns the BoostTracker for the given car. */
  public static BoostTracker forCar(CarData car) {
    if (!car.isLiveData) {
      throw new IllegalStateException("Not supported for non-live data");
    }

    return MAP.computeIfAbsent(car.serialNumber, index -> new BoostTracker());
  }

  /**
   * Returns a copy of the Boost tracker for this car.
   */
  public static BoostTracker copyForCar(CarData car) {
    return forCar(car).copy();
  }

  private int boostTicks;

  /**
   * Updates the boost tracker at the end of the frame.
   */
  public void update(CarData car, Controls output) {
    if (car.boost == 0) {
      boostTicks = 0;
    } else if (output.holdBoost()) {
      boostTicks++;
    } else if (boostTicks > 0 && boostTicks <= 12) {
      boostTicks++;
    } else {
      boostTicks = 0;
    }
  }

  /**
   * True if the car was boosting this frame.
   */
  public boolean isBoosting() {
    return boostTicks != 0;
  }

  /**
   * Creates a copy of this boost tracker for use in simulations.
   */
  private BoostTracker copy() {
    BoostTracker boostTracker = new BoostTracker();
    boostTracker.boostTicks = this.boostTicks;
    return boostTracker;
  }

  /**
   * Returns the minimum number of frames boosting this frame will be active.
   */
  public int getCommitment() {
    return Math.max(1, 12 - boostTicks);
  }
}
