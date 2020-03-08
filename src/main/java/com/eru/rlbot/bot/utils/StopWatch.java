package com.eru.rlbot.bot.utils;

import static com.eru.rlbot.bot.common.Constants.NANOS;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility for timing and logging code blocks.
 */
public final class StopWatch {

  private static final Logger logger = LogManager.getLogger("StopWatch");

  private final String label;
  private final long start;
  private long end;

  private StopWatch(String label) {
    this.label = label;
    this.start = System.nanoTime();
  }

  /**
   * Creates a started stop-watch.
   */
  public static StopWatch start(String label) {
    return new StopWatch(label);
  }

  /**
   * Stops and logs the given action. Returns the time in seconds.
   */
  public double stop() {
    if (end == 0) {
      end = System.nanoTime();
      logger.debug("{}: {} ms", label, (end - start) / NANOS);
    }

    return (end - start) / NANOS;
  }
}
