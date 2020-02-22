package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Monitor {

  private static final Logger logger = LogManager.getLogger("Monitor");

  private final CarData start;
  private CarData end;

  public static Monitor create(DataPacket input) {
    return create(input.car);
  }

  public static Monitor create(CarData car) {
    return new Monitor(car);
  }

  public Monitor(CarData initial) {
    this.start = initial;
  }

  public void trackWhile(boolean value, CarData car) {
    if (end != null) {
      return;
    }

    if (!value) {
      this.end = car;

      logger.info("Time: {}", end.elapsedSeconds - start.elapsedSeconds);
      logger.info("Pos: {}", end.position.minus(start.position));
      logger.info("Vel: {}", end.velocity.minus(start.velocity));
      logger.info("Boost: {}", end.boost - start.boost);
      logger.info("aVel: {}", end.angularVelocity.minus(start.angularVelocity));
      logger.info("Orientation: {}", end.orientation.getOrientationMatrix()
          .minus(start.orientation.getOrientationMatrix()));
      logger.info("relative position: {}", NormalUtils.translateRelative(start.position, end.position, start.orientation.getNoseVector()));
    }
  }
}
