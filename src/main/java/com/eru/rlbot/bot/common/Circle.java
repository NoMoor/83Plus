package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.vector.Vector3;

/**
 * Represents a circle in the xy coordinate space on the field.
 */
public class Circle {

  public final Vector3 center;
  public final double radius;
  public final double maxSpeed;

  public Circle(Vector3 center, double radius, double maxSpeed) {
    this.center = center;
    this.radius = radius;
    this.maxSpeed = maxSpeed;
  }
}
