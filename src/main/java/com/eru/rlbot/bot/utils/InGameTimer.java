package com.eru.rlbot.bot.utils;

import com.eru.rlbot.common.input.DataPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Times events using game time.
 */
public class InGameTimer {

  private static final Logger logger = LogManager.getLogger("InGameTimer");

  private final String label;
  private final float start;

  private InGameTimer(DataPacket input, String label) {
    this.start = input.car.elapsedSeconds;
    this.label = label;
  }

  /** Creates and returns a started timer. */
  public static InGameTimer start(DataPacket input, String label) {
    return new InGameTimer(input, label);
  }

  /** Stops and logs the given time. The time is also returned. */
  public double stop(DataPacket input) {
    double duration = input.car.elapsedSeconds - start;
    logger.info("{}: {}s", label, String.format("%.2f", duration));
    return duration;
  }
}
