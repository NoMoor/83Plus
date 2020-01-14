package com.eru.rlbot.common.boost;

import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.output.ControlsOutput;
import java.util.HashMap;

public class BoostTracker {

  private static HashMap<Integer, BoostTracker> TRACKER_MAP = new HashMap<>();

  public static BoostTracker forCar(CarData car) {
    if (!car.isLiveData) {
      throw new IllegalStateException("Not supported for non-live data");
    }

    return TRACKER_MAP.computeIfAbsent(car.playerIndex, index -> new BoostTracker());
  }

  public static BoostTracker copyForCar(CarData car) {
    return forCar(car).copy();
  }

  private int boostTicks;

  public void update(CarData car, ControlsOutput output) {
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

  public boolean isBoosting() {
    return boostTicks != 0;
  }

  public BoostTracker copy() {
    BoostTracker boostTracker = new BoostTracker();
    boostTracker.boostTicks = this.boostTicks;
    return boostTracker;
  }
}
