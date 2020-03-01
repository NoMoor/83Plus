package com.eru.rlbot.bot.common;

import static com.eru.rlbot.bot.common.Constants.NANOS;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility for timing and logging code blocks
 */
public class StopWatch {

  private static final Logger logger = LogManager.getLogger("StopWatch");

  private final String label;
  private final long start;

  public StopWatch(String label) {
    this.label = label;
    this.start = System.nanoTime();
  }

  public static StopWatch start(String label) {
    return new StopWatch(label);
  }

  public void stop() {
    double duration = (System.nanoTime() - start) / NANOS;
    logger.info("{}: {} ms", label, duration);
  }
}
