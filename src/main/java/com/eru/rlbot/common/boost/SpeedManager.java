package com.eru.rlbot.common.boost;

import com.eru.rlbot.common.input.DataPacket;

public class SpeedManager {

  private static final double SUPER_SONIC_SPEED = 2200;

  private static float superSonicTime;
  private static float timeSuperSonic;

  private static double speed;

  public static void trackSuperSonic(DataPacket input) {
    speed = input.car.velocity.flatten().norm();

    if (input.car.isSupersonic) {
      if (superSonicTime == 0) {
        superSonicTime = input.car.elapsedSeconds;
      } else {
        timeSuperSonic = superSonicTime - input.car.elapsedSeconds;
      }
    } else {
      superSonicTime = 0;
      timeSuperSonic = 0;
    }
  }

  public static boolean isSuperSonic() {
    return superSonicTime != 0;
  }
}
