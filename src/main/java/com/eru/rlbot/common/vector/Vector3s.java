package com.eru.rlbot.common.vector;

/**
 * Utility methods for vectors in R3.
 */
public final class Vector3s {

  // https://math.stackexchange.com/a/2193733
  public static double shortestDistanceToVector(Vector3 location, Vector3 start, Vector3 end) {
    Vector3 closestPoint = nearestPointOnLineSegment(location, start, end);
    if (closestPoint.equals(start) || closestPoint.equals(end)) {
      return Double.MAX_VALUE;
    }
    return location.distance(closestPoint);
  }

  public static Vector3 nearestPointOnLineSegment(Vector3 location, Vector3 start, Vector3 end) {
    Vector3 v = end.minus(start);
    Vector3 u = start.minus(location);

    double t = -(v.dot(u)) / (v.dot(v));
    if (t >= 0 && t <= 1) {
      return start.multiply(1 - t).plus(end.multiply(t));
    } else {
      return start.distance(location) < end.distance(location) ? start : end;
    }
  }

  private Vector3s() {}
}
