package com.eru.rlbot.common;

/**
 * Utility methods for numbers.
 */
public final class Numbers {

  /**
   * Clamps the number between the given bounds.
   */
  public static double clamp(double value, double min, double max) {
    return Math.min(max, Math.max(min, value));
  }

  /**
   * Clamps the number between the given bounds.
   */
  public static float clamp(float value, float min, float max) {
    return Math.min(max, Math.max(min, value));
  }

  /**
   * Linearly interpolates between a and b the given amount.
   */
  public static double lerp(double a, double b, double lerpAmount) {
    return a + ((b - a) * lerpAmount);
  }

  /**
   * Linearly interpolates between a and b the given amount.
   */
  public static float lerp(float a, float b, float lerpAmount) {
    return a + ((b - a) * lerpAmount);
  }

  private Numbers() {
  }
}
