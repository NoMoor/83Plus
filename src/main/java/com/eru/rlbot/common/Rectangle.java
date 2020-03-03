package com.eru.rlbot.common;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;
import java.util.Comparator;

/**
 * A rectangle in 3d space.
 */
public class Rectangle {

  private final Vector3 a;
  private final Vector3 b;
  private final Vector3 c;
  private final Vector3 d;

  private Rectangle(Vector3 a, Vector3 b, Vector3 c, Vector3 d) {
    this.a = a;
    this.b = b;
    this.c = c;
    this.d = d;
  }

  /** Constructs a rectangle in space. <b>Note:</b> No validation of the shape is actually done. */
  public static Rectangle from(Vector3 a, Vector3 b, Vector3 c, Vector3 d) {
    return new Rectangle(a, b, c, d);
  }

  public double area() {
    double ab = a.distance(b);
    double ac = a.distance(c);
    double ad = a.distance(d);

    ImmutableList<Double> lenWidth = ImmutableList.of(ab, ac, ad).stream()
        .sorted(Comparator.naturalOrder())
        .limit(2)
        .collect(toImmutableList());

    return lenWidth.get(0) * lenWidth.get(1);
  }
}
