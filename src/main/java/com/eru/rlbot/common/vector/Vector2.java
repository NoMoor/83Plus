package com.eru.rlbot.common.vector;

/**
 * A vector that only knows about x and y components.
 */
public class Vector2 {

  public static final Vector2 NORTH = Vector2.of(0, 1);
  public static final Vector2 WEST = Vector2.of(1, 0);

  public final double x;
  public final double y;

  /** Creates a new vector from the given x and y coordinates. */
  public static Vector2 of(double x, double y) {
    return new Vector2(x, y);
  }

  private Vector2(double x, double y) {
    this.x = x;
    this.y = y;
  }

  /**
   * Adds two vectors tip to tail.
   */
  public Vector2 plus(Vector2 other) {
    return Vector2.of(x + other.x, y + other.y);
  }

  /**
   * Subtracts the given vector from this one.
   */
  public Vector2 minus(Vector2 other) {
    return Vector2.of(x - other.x, y - other.y);
  }

  /**
   * Scales this vector by the given scalar.
   */
  public Vector2 multiplied(double scale) {
    return Vector2.of(x * scale, y * scale);
  }

  /**
   * If magnitude is negative, we will return a vector facing the opposite direction.
   */
  public Vector2 toMagnitude(double magnitude) {
    if (isZero()) {
      throw new IllegalStateException("Cannot scale up a vector with length zero!");
    }
    double scaleRequired = magnitude / magnitude();
    return multiplied(scaleRequired);
  }

  /** Returns the distance between this vector coordinate and the given one. */
  public double distance(Vector2 other) {
    double xDiff = x - other.x;
    double yDiff = y - other.y;
    return Math.sqrt(xDiff * xDiff + yDiff * yDiff);
  }

  /**
   * This is the length of the vector.
   */
  public double magnitude() {
    return Math.sqrt(x * x + y * y);
  }

  /** Returns this vector scaled to a magnitude of one. */
  public Vector2 normalized() {

    if (isZero()) {
      throw new IllegalStateException("Cannot normalize a vector with length zero!");
    }
    return this.multiplied(1 / magnitude());
  }

  public double dotProduct(Vector2 other) {
    return x * other.x + y * other.y;
  }

  /** True if both parts of this vector are zero. */
  public boolean isZero() {
    return x == 0 && y == 0;
  }

  /**
   * The correction angle is how many radians you need to rotate this vector to make it line up with the "ideal"
   * vector. This is very useful for deciding which direction to steer.
   */
  public double correctionAngle(Vector2 ideal) {
    double currentRad = Math.atan2(y, x);
    double idealRad = Math.atan2(ideal.y, ideal.x);

    if (Math.abs(currentRad - idealRad) > Math.PI) {
      if (currentRad < 0) {
        currentRad += Math.PI * 2;
      }
      if (idealRad < 0) {
        idealRad += Math.PI * 2;
      }
    }

    return idealRad - currentRad;
  }

  /**
   * Will always return a positive value <= Math.PI
   */
  public static double angle(Vector2 a, Vector2 b) {
    return Math.abs(a.correctionAngle(b));
  }

  /**
   * Returns this vector as a flat 3D vecotyr.
   */
  public Vector3 asVector3() {
    return Vector3.of(x, y, 0);
  }

  /**
   * Returns a vector perpendicular to this one.
   */
  public Vector2 clockwisePerpendicular() {
    return Vector2.of(-y, x);
  }
}
