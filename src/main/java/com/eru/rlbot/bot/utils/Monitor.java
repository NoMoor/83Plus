package com.eru.rlbot.bot.utils;

import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Monitor {

  private static final Logger logger = LogManager.getLogger("Monitor");

  public final CarData start;
  private CarData end;

  public static Monitor create(DataPacket input) {
    return create(input.car);
  }

  public static Monitor create(CarData car) {
    return new Monitor(car);
  }

  private Monitor(CarData initial) {
    this.start = initial;
  }

  public void trackWhile(boolean value, CarData car) {
    if (end != null) {
      return;
    }

    if (!value) {
      this.end = car;

      logger.debug("Time: {}", end.elapsedSeconds - start.elapsedSeconds);
      logger.debug("Pos: {}", end.position.minus(start.position));
      logger.debug("Vel: {}", end.velocity.minus(start.velocity));
      logger.debug("Boost: {}", end.boost - start.boost);
      logger.debug("aVel: {}", end.angularVelocity.minus(start.angularVelocity));
      logger.debug("Orientation: {}", end.orientation.getOrientationMatrix()
          .minus(start.orientation.getOrientationMatrix()));
    }
  }
}
