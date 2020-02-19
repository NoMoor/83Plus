package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.vector.Vector3;

/** A class that represents a 3x3 matrix. */
public class Matrix3 {

  /** The 3x3 identity matrix. */
  public static final Matrix3 IDENTITY = Matrix3.of(
      Vector3.of(1, 0, 0),
      Vector3.of(0, 1, 0),
      Vector3.of(0, 0, 1));

  /** Creates a new matrix from the given row vectors. */
  public static Matrix3 of(Vector3 a, Vector3 b, Vector3 c) {
    return new Matrix3(a, b, c);
  }

  private final Vector3 a;
  private final Vector3 b;
  private final Vector3 c;

  protected Matrix3(Vector3 a, Vector3 b, Vector3 c) {
    this.a = a;
    this.b = b;
    this.c = c;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof Matrix3)) {
      return false;
    }

    Matrix3 m = (Matrix3) o;

    return a.equals(m.a) && b.equals(m.b) && c.equals(m.c);
  }

  /** Returns the vector formed by all the values in a given row. */
  public Vector3 row(int i) {
    switch (i) {
      case 0:
        return a;
      case 1:
        return b;
      case 2:
        return c;
      default:
        throw new IllegalStateException(String.format("No index for this matrix at %d", i));
    }
  }

  /** Returns a vector formed by all the values in a given column. */
  public Vector3 column(int i) {
    return Vector3.of(a.get(i), b.get(i), c.get(i));
  }

  /** Transposes the matrix across the diagonal indexes. */
  private Matrix3 transpose;
  public Matrix3 transpose() {
    if (transpose == null) {
      transpose = new Matrix3(
          Vector3.of(a.x, b.x, c.x),
          Vector3.of(a.y, b.y, c.y),
          Vector3.of(a.z, b.z, c.z));
    }
    return transpose;
  }

  /**
   * Returns the inverse of the matrix.
   */
  private Matrix3 inverse;

  @Override
  public String toString() {
    return "[" + a + "," + b + "," + c + "]";
  }

  public Matrix3 inverse() {
    if (inverse == null) {
      float determinant = determinant();

      if (determinant() == 0) {
        throw new IllegalArgumentException("There is no inverse for this matrix " + toString());
      }

      Matrix3 transposition = transpose();

      float inv_determinant = 1 / determinant;

      inverse = Matrix3.of(
          Vector3.of(
              transposition.minorDeterminant(0, 0) * inv_determinant,
              transposition.minorDeterminant(0, 1) * -inv_determinant,
              transposition.minorDeterminant(0, 2) * inv_determinant),
          Vector3.of(
              transposition.minorDeterminant(1, 0) * -inv_determinant,
              transposition.minorDeterminant(1, 1) * inv_determinant,
              transposition.minorDeterminant(1, 2) * -inv_determinant),
          Vector3.of(
              transposition.minorDeterminant(2, 0) * inv_determinant,
              transposition.minorDeterminant(2, 1) * -inv_determinant,
              transposition.minorDeterminant(2, 2) * inv_determinant));
    }
    return inverse;
  }

  private float minorDeterminant(int y, int x) {
    if (x < 0 || x > 2 || y < 0 || y > 2) {
      throw new IllegalArgumentException("Indexes out of bounds: " + x + " " + y);
    }

    int left = x == 0 ? 1 : 0;
    int right = x == 2 ? 1 : 2;
    int top = y == 0 ? 1 : 0;
    int bottom = y == 2 ? 1 : 2;

    return (row(top).get(left) * row(bottom).get(right)) - (row(top).get(right) * row(bottom).get(left));
  }

  /** Returns the determinant of this matrix. */
  private Float determinant;
  public float determinant() {
    if (determinant == null) {
      float deta = a.x * ((b.y * c.z) - (b.z * c.y));
      float detb = -a.y * ((b.x * c.z) - (b.z * c.x));
      float detc = a.z * ((b.x * c.y) - (b.y * c.x));

      determinant = deta + detb + detc;
    }
    return determinant;
  }

  /** The dot product of this matrix and the given vector. */
  public Vector3 dot(Vector3 v) {
    return Vector3.of(a.dot(v), b.dot(v), c.dot(v));
  }

  /** The dot product of this matrix and the given matrix. */
  public Matrix3 dot(Matrix3 m) {
    return new Matrix3(
        Vector3.of(a.dot(m.column(0)), a.dot(m.column(1)), a.dot(m.column(2))),
        Vector3.of(b.dot(m.column(0)), b.dot(m.column(1)), b.dot(m.column(2))),
        Vector3.of(c.dot(m.column(0)), c.dot(m.column(1)), c.dot(m.column(2))));
  }

  /** Returns true if this is equal to the identity matrix. */
  public boolean isIdentity() {
    return a.equals(IDENTITY.a) && b.equals(IDENTITY.b) && c.equals(IDENTITY.c);
  }

  /** The trace (ie. sum of the main diagonal) of this matrix. */
  public float trace() {
    return a.x + b.y + c.z;
  }

  /** Scales this matrix by the given scalar. */
  public Matrix3 multiply(double scale) {
    return Matrix3.of(a.multiply(scale), b.multiply(scale), c.multiply(scale));
  }

  /** Scales this matrix by the inverse of the given scalar. */
  public Matrix3 divide(double scale) {
    return this.multiply(1 / scale);
  }

  /** Subtracts the given matrix from this one. */
  public Matrix3 minus(Matrix3 other) {
    return Matrix3.of(a.minus(other.a), b.minus(other.b), c.minus(other.c));
  }

  /** Adds the given matrix from this one. */
  public Matrix3 plus(Matrix3 other) {
    return Matrix3.of(a.plus(other.a), b.plus(other.b), c.plus(other.c));
  }
}
