package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.input.DataPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InGameTimer {

  private static final Logger logger = LogManager.getLogger("InGameTimer");

  private final String label;
  private final float start;

  public InGameTimer(DataPacket input, String label) {
    this.start = input.car.elapsedSeconds;
    this.label = label;
  }

  public static InGameTimer start(DataPacket input, String label) {
    return new InGameTimer(input, label);
  }

  public double stop(DataPacket input) {
    double duration = input.car.elapsedSeconds - start;
    logger.info("{}: {}s", label, String.format("%.2f", duration));
    return duration;
  }
}
