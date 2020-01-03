package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;

/**
 * Represents a circle in the xy coordinate space on the field.
 */
public class Circle {

  public final Vector3 center;
  public final double radius;
  public final double maxSpeed;

  public Circle(Vector3 center, double radius) {
    this.center = center;
    this.radius = radius;
    this.maxSpeed = Constants.maxSpeed(radius) * .95;
  }

  public static Vector3 pointOnCircle(Vector3 center, double radius, double radians) {
    double x = center.x + (radius * Math.cos(radians));
    double y = center.y + (radius * Math.sin(radians));

    return Vector3.of(x, y, center.z);
  }

  public boolean isClockwise(CarData carData) {
    return isClockwise(carData.position, carData.orientation.getNoseVector());
  }

  private boolean isClockwise(Vector3 position, Vector3 noseVector) {
    return center.minus(position).cross(noseVector).z < 0;
  }

  public double radianOffset(Vector3 point) {
    Vector2 ray = point.minus(center).flatten();
    return Vector2.WEST.correctionAngle(ray);
  }
}
