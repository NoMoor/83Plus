package com.eru.rlbot.bot.flags;

/**
 * A number of values used for debugging purposes. These can be adjusted by the UI and affect all cars.
 */
public class GlobalDebugOptions {

  private static boolean slowTimeNearBallEnabled = false;
  private static boolean kickoffGameEnabled = false;
  private static boolean stateLoggerEnabled = false;
  private static int slowedGameSpeed = 5; // in [0, 10]

  public static boolean isSlowTimeNearBallEnabled() {
    return slowTimeNearBallEnabled;
  }

  public static void setSlowTimeNearBallEnabled(boolean slowTimeNearBallEnabled) {
    GlobalDebugOptions.slowTimeNearBallEnabled = slowTimeNearBallEnabled;
  }

  public static boolean isKickoffGameEnabled() {
    return kickoffGameEnabled;
  }

  public static void setKickoffGameEnabled(boolean kickoffGameEnabled) {
    GlobalDebugOptions.kickoffGameEnabled = kickoffGameEnabled;
  }

  public static boolean isStateLoggerEnabled() {
    return stateLoggerEnabled;
  }

  public static void setStateLoggerEnabled(boolean stateLoggerEnabled) {
    GlobalDebugOptions.stateLoggerEnabled = stateLoggerEnabled;
  }

  public static int getSlowedGameSpeed() {
    return slowedGameSpeed;
  }

  public static void setSlowedGameSpeed(int slowedGameSpeed) {
    GlobalDebugOptions.slowedGameSpeed = slowedGameSpeed;
  }
}
