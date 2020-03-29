package com.eru.rlbot.common.vector;

import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.common.Matrix3;
import com.google.common.base.Objects;
import com.google.flatbuffers.FlatBufferBuilder;
import rlbot.gamestate.DesiredVector3;

/**
 * A simple 3d vector class with the most essential operations.
 */
public class Vector3 extends rlbot.vector.Vector3 {

  public static final Vector3 WEST = Vector2.WEST.asVector3();

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

  /**
   * Returns a new vector pointing from source to target.
   */
  public static Vector3 from(Vector3 source, Vector3 target) {
    return target.minus(source);
  }

  /**
   * Creates a Vector3 at car height.
   */
  public static Vector3 fieldLevel(double x, double y) {
    return of(x, y, Constants.CAR_AT_REST);
  }

  /**
   * Translates a vector to the framework flat vector format.
   */
  public int toFlatbuffer(FlatBufferBuilder builder) {
    return rlbot.flat.Vector3.createVector3(builder, x, y, z);
  }

  /**
   * Adds the two vectors together.
   */
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

  /** Scales the vector by the inverse of the given value. */
  public Vector3 divide(double scale) {
    return this.multiply(1 / scale);
  }

  /**
   * If magnitude is negative, we will return a vector facing the opposite direction.
   */
  public Vector3 toMagnitude(double magnitude) {
    if (isZero()) {
      throw new IllegalStateException("Cannot scale up a vector with length zero!");
    }
    return toMagnitudeUnchecked(magnitude);
  }

  public Vector3 toMagnitudeUnchecked(double magnitude) {
    if (isZero()) {
      return this;
    }
    double scaleRequired = magnitude / magnitude();
    return multiply(scaleRequired);
  }


  /**
   * Returns the distance between the two vector locations.
   */
  public double distance(Vector3 other) {
    double xDiff = x - other.x;
    double yDiff = y - other.y;
    double zDiff = z - other.z;
    return Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff);
  }

  /**
   * Returns magnitude of the vector.
   */
  public double magnitude() {
    return Math.sqrt(this.dot(this));
  }

  /**
   * Returns the vector normalize to magnitude 1.
   */
  public Vector3 normalize() {

    if (isZero()) {
      throw new IllegalStateException("Cannot normalize a vector with length zero!");
    }
    return this.multiply(1 / magnitude());
  }

  public Vector3 normalizeOrZero() {

    if (isZero()) {
      return Vector3.zero();
    }
    return this.multiply(1 / magnitude());
  }

  public Vector3 uncheckedNormalize() {
    if (isZero()) {
      return new Vector3(1, 0, 0);
    }
    return this.multiply(1 / magnitude());
  }

  /**
   * Returns the dot-product of this vector with the given vector.
   */
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
    return Vector2.of(x, y);
  }

  /**
   * Angle in radians.
   */
  public double angle(Vector3 v) {
    double mag = magnitude();
    double vmag = v.magnitude();
    double dot = dot(v);
    return Math.acos(dot / (mag * vmag));
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

  @Override
  public int hashCode() {
    return Objects.hashCode(x, y, z);
  }

  @Override
  public String toString() {
    return String.format("[%f,%f,%f]", x, y, z);
  }

  /**
   * Returns the i-th element of this vector.
   */
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

  /**
   * Returns a new vector with value added to the x component.
   */
  public Vector3 addX(double value) {
    return new Vector3(this.x + value, y, z);
  }

  /**
   * Returns a new vector with x set to the given value.
   */
  public Vector3 setX(double value) {
    return new Vector3(value, y, z);
  }

  /**
   * Returns a new vector with value added to the y component.
   */
  public Vector3 addY(double value) {
    return new Vector3(this.x, this.y + value, z);
  }

  /**
   * Returns a new vector with y set to the given value.
   */
  public Vector3 setY(double value) {
    return new Vector3(x, value, z);
  }

  /**
   * Returns a new vector with value added to the z component.
   */
  public Vector3 addZ(double value) {
    return new Vector3(x, y, z + value);
  }

  /**
   * Returns a new vector with z set to the given value.
   */
  public Vector3 setZ(double value) {
    return new Vector3(x, y, value);
  }

  public Vector3 flat() {
    return Vector3.of(this.x, this.y, 0);
  }

  public Vector3 setZ(float newZ) {
    return new Vector3(this.x, this.y, newZ);
  }

  public Vector3 clockwisePerpendicular() {
    // TODO: Verify that this is correct. ... This seems wrong.
    return this.cross(Vector3.of(0, 0, 1));
  }

  public RoughComparator isWithin(double value) {
    return this.new RoughComparator(value);
  }

  public DesiredVector3 toDesired() {
    return new DesiredVector3()
        .withX(x)
        .withY(y)
        .withZ(z);
  }

  public boolean isNan() {
    return Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z);
  }

  public class RoughComparator {

    private final double tolerance;

    private RoughComparator(double tolerance) {
      this.tolerance = tolerance;
    }

    public boolean of(Vector3 vector3) {
      return Math.abs(Vector3.this.x - vector3.x) < tolerance
          && Math.abs(Vector3.this.y - vector3.y) < tolerance
          && Math.abs(Vector3.this.z - vector3.z) < tolerance;
    }
  }
}
