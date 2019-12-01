package com.eru.rlbot.common.vector;

import com.eru.rlbot.bot.common.Matrix3;
import com.google.flatbuffers.FlatBufferBuilder;

/**
 * A simple 3d vector class with the most essential operations.
 *
 * This class is here for your convenience, it is NOT part of the framework. You can add to it as much
 * as you want, or delete it.
 */
public class Vector3 extends rlbot.vector.Vector3 {

  /** Creates the null vector. */
  public static Vector3 zero() {
    return of(0d, 0d, 0d);
  }

  /** Creates a vector from the flat rlbot framework. */
  public static Vector3 of(rlbot.flat.Vector3 vec) {
    return new Vector3(vec);
  }

  /** Creates a vector from the given x, y, z values. */
  public static Vector3 of(double x, double y, double z) {
    return new Vector3(x, y, z);
  }

  private Vector3(double x, double y, double z) {
    super((float) x, (float) y, (float) z);
  }

  private Vector3(rlbot.flat.Vector3 vec) {
    this(vec.x(), vec.y(), vec.z());
  }

  /** Returns a new vector pointing from source to target. */
  public static Vector3 from(Vector3 source, Vector3 target) {
    return target.minus(source);
  }

  /** Translates a vector to the framework flat vector format. */
  public int toFlatbuffer(FlatBufferBuilder builder) {
    return rlbot.flat.Vector3.createVector3(builder, x, y, z);
  }

  /** Adds the two vectors together. */
  public Vector3 plus(Vector3 other) {
    return new Vector3(x + other.x, y + other.y, z + other.z);
  }

  /** Subtracts the two vectors from one another. */
  public Vector3 minus(Vector3 other) {
    return new Vector3(x - other.x, y - other.y, z - other.z);
  }

  /** Scales the vector by the given value. */
  public Vector3 multiply(double scale) {
    return new Vector3(x * scale, y * scale, z * scale);
  }

  /**
   * If norm is negative, we will return a vector facing the opposite direction.
   */
  public Vector3 toMagnitude(double magnitude) {
    if (isZero()) {
      throw new IllegalStateException("Cannot scale up a vector with length zero!");
    }
    double scaleRequired = magnitude / norm();
    return multiply(scaleRequired);
  }

  /** Returns the distance between the two vector locations. */
  public double distance(Vector3 other) {
    double xDiff = x - other.x;
    double yDiff = y - other.y;
    double zDiff = z - other.z;
    return Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff);
  }

  /** Returns norm of the vector. */
  public double norm() {
    return Math.sqrt(this.dot(this));
  }

  /** Returns the vector normalized to norm 1. */
  public Vector3 normalized() {

    if (isZero()) {
      throw new IllegalStateException("Cannot normalize a vector with length zero!");
    }
    return this.multiply(1 / norm());
  }

  /** Returns the dot-product of this vector with the given vector. */
  public double dot(Vector3 other) {
    return x * other.x + y * other.y + z * other.z;
  }

  /** Returns the dot-product of this vector with the given matrix. */
  public Vector3 dot(Matrix3 mat) {
    return Vector3.of(
        this.dot(Vector3.of(mat.row(0).x, mat.row(1).x, mat.row(2).x)),
        this.dot(Vector3.of(mat.row(0).y, mat.row(1).y, mat.row(2).y)),
        this.dot(Vector3.of(mat.row(0).z, mat.row(1).z, mat.row(2).z)));
  }

  /** Returns true if this vector is the zero vector. */
  public boolean isZero() {
    return x == 0 && y == 0 && z == 0;
  }

  /** Returns a 2-d vector, dropping this z axis. */
  public Vector2 flatten() {
    return new Vector2(x, y);
  }

  /** Angle in radians? */
  public double angle(Vector3 v) {
    double mag2 = norm();
    double vmag2 = v.norm();
    double dot = dot(v);
    return Math.acos(dot / Math.sqrt(mag2 * vmag2));
  }

  /** Returns the cross product of this and the given vector. */
  public Vector3 cross(Vector3 v) {
    double tx = y * v.z - z * v.y;
    double ty = z * v.x - x * v.z;
    double tz = x * v.y - y * v.x;
    return new Vector3(tx, ty, tz);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (o instanceof Vector3) {
      Vector3 v = (Vector3) o;
      return this.x == v.x && this.y == v.y && this.z == v.z;
    }
    return false;
  }

  /** Returns the i-th element of this vector. */
  public float get(int i) {
    switch (i) {
      case 0:
        return x;
      case 1:
        return y;
      case 2:
        return z;
      default:
        throw new IllegalStateException(String.format("No index for this matrix at %d", i));
    }
  }

  /** Returns a new vector with value added to the x component. */
  public Vector3 addX(float value) {
    return new Vector3(this.x + value, y, z);
  }

  /** Returns a new vector with value added to the y component. */
  public Vector3 addY(float value) {
    return new Vector3(this.x, this.y + value, z);
  }

  /** Returns a new vector with value added to the z component. */
  public Vector3 addZ(float value) {
    return new Vector3(this.x, y, this.z + value);
  }
}
