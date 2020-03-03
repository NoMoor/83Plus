package com.eru.rlbot.bot.path;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.eru.rlbot.bot.common.Circle;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.stream.Stream;

public class Paths {

  public static Circle turnRadius(CarData car, Vector3 targetLocation) {
    Vector3 front = car.orientation.getNoseVector().normalize();
    Vector3 up = front.cross(Vector3.of(0, 0, 1).cross(front)).normalize();
    Vector3 left = up.cross(front).normalize();

    double turnRadius = Circle.radiusForPath(car.groundSpeed);

    Vector3 centerOffset = left.toMagnitude(turnRadius);

    Vector3 center1 = car.position.plus(centerOffset);
    Vector3 center2 = car.position.minus(centerOffset);
    return Circle.forPath(center1.distance(targetLocation) < center2.distance(targetLocation) ? center1 : center2, turnRadius);
  }

  /**
   * Returns the ideal approach based on the target speed.
   */
  static Circles closeApproach(CarData target) {
    Vector3 front = target.orientation.getNoseVector().normalize();
    Vector3 up = front.cross(Vector3.of(0, 0, 1).cross(front)).normalize();
    Vector3 left = up.cross(front).normalize();

    double turnRadius = Circle.radiusForPath(target.groundSpeed);

    Vector3 centerOffset = left.toMagnitude(turnRadius);

    Vector3 center1 = target.position.plus(centerOffset);
    Vector3 center2 = target.position.minus(centerOffset);

    return new Circles(
        Circle.forPath(center1, turnRadius),
        Circle.forPath(center2, turnRadius));
  }

  static Vector3 tangentForTargetDirection(Circle circle, Vector3 currentCarPos, CarData targetCar) {
    TangentPoints tangentPoints = tangents(circle, currentCarPos);

    // Select the point going in the right direction.
    Vector3 centerToTarget = circle.center.minus(targetCar.position);
    double targetHandedness = Math.signum(centerToTarget.cross(targetCar.orientation.getNoseVector()).z);

    return targetHandedness < 0 ? tangentPoints.left : tangentPoints.right;
  }

  public static ImmutableList<Segment> shortestBiArc(CarData target, CarData source) {
    Circles ballStrikingCircles = closeApproach(target);
    Circles carTurningCircles =
        turningRadiusCircles(source.position, Math.max(800, source.groundSpeed), source.orientation.getNoseVector());

    ImmutableList<Segment> cwcw = biArcSegments(source.position, carTurningCircles.cw, target.position, ballStrikingCircles.cw, CircleTangents.Shape.CWCW);
    ImmutableList<Segment> ccwcw = biArcSegments(source.position, carTurningCircles.ccw, target.position, ballStrikingCircles.cw, CircleTangents.Shape.CCWCW);
    ImmutableList<Segment> cwccw = biArcSegments(source.position, carTurningCircles.cw, target.position, ballStrikingCircles.ccw, CircleTangents.Shape.CWCCW);
    ImmutableList<Segment> ccwccw = biArcSegments(source.position, carTurningCircles.ccw, target.position, ballStrikingCircles.ccw, CircleTangents.Shape.CCWCCW);

    return Stream.of(cwcw, ccwcw, cwccw, ccwccw)
        .min(Comparator.comparingDouble(segments -> segments.stream().mapToDouble(Segment::flatDistance).sum()))
        .get();
  }

  private static double MIN_SEGMENT_LENGTH = 100;
  private static ImmutableList<Segment> biArcSegments(
      Vector3 start, Circle circle1, Vector3 end, Circle circle2, CircleTangents.Shape shape) {
    Segment connector = tangents(circle1, circle2).getSegment(shape);
    Segment arc1 = Segment.arc(start, connector.start, circle1, shape.startsClockWise());
    Segment arc2 = Segment.arc(connector.end, end, circle2, shape.endsClockWise());

    ImmutableList.Builder<Segment> segmentListBuilder = ImmutableList.builder();

    // Check arc 1 for inclusion
    if (arc1.flatDistance() > MIN_SEGMENT_LENGTH &&
        Math.abs(arc1.circle.circumference - arc1.flatDistance()) > MIN_SEGMENT_LENGTH) {

      segmentListBuilder.add(arc1);
    } else {
      // Else amend the connector
      connector = Segment.straight(start, connector.end);
    }

    if (arc2.flatDistance() > MIN_SEGMENT_LENGTH &&
        Math.abs(arc2.circle.circumference - arc2.flatDistance()) > MIN_SEGMENT_LENGTH) {

      segmentListBuilder
          .add(connector)
          .add(arc2);
    } else {
      // Add adjusted connector without the second arc.
      segmentListBuilder
          .add(Segment.straight(connector.start, arc2.end));
    }

    return segmentListBuilder.build();
  }

  public static TangentPoints tangents(Circle circle, Vector3 point) {
    // Find a circle that goes through both the center of the original circle and the given position.
    Vector3 bisector = circle.center.minus(point).divide(2);

    double d = bisector.magnitude();
    double r = circle.radius;
    double R = d; // The larger circle goes through the smaller.

    double x = ((d * d) - (r * r) + (R * R)) / (2 * d);
    double a = (1 / (2 * d)) * Math.sqrt((4 * (d * d) * (R * R)) - Math.pow((d * d) - (r * r) + (R * R), 2));

    Vector3 tanx = point.plus(bisector.plus(bisector.normalize().toMagnitude(x)));
    Vector3 perp = bisector.flatten().clockwisePerpendicular().toMagnitude(a).asVector3();

    return new TangentPoints(tanx.minus(perp), tanx.plus(perp));
  }

  public static CircleTangents tangents(Circle a, Circle b) {
    if (Math.abs(a.radius - b.radius) < 1) {
      a = Circle.forPath(a.center, a.radius + 2);
    }
    Circle larger = a.radius > b.radius ? a : b;
    Circle smaller = larger == a ? b : a;
    boolean aIsLarger = a.radius > b.radius;

    // TODO: Do checking to see if they overlap.

    // Get inside tangents.
    ImmutableList<Segment> insideSegments =
        tangents(Circle.forPath(larger.center, larger.radius + smaller.radius), smaller.center).getPoints().stream()
            .map(tangentPoint -> toInsideSegments(larger, smaller, aIsLarger, tangentPoint))
            .collect(toImmutableList());

    // Get outside tangents.
    ImmutableList<Segment> outsideSegments =
        tangents(Circle.forPath(larger.center, larger.radius - smaller.radius), smaller.center).getPoints().stream()
//            .filter(tangentPoint -> !larger.center.equals(tangentPoint))
            .map(tangentPoint -> toOutsideSegments(larger, smaller, aIsLarger, tangentPoint))
            .collect(toImmutableList());

    return new CircleTangents(
        outsideSegments.get(0),
        insideSegments.get(0),
        insideSegments.get(1),
        outsideSegments.get(1));
  }

  private static Segment toOutsideSegments(Circle larger, Circle smaller, boolean aIsLarger, Vector3 tangentPoint) {
    try {
      Vector3 offset = larger.center.minus(tangentPoint).toMagnitude(smaller.radius);
      Vector3 largerTangent = tangentPoint.minus(offset);
      Vector3 smallerTangent = smaller.center.minus(offset);
      return Segment.straight(
          aIsLarger ? largerTangent : smallerTangent,
          aIsLarger ? smallerTangent : largerTangent);
    } catch (IllegalStateException e) {
      throw e;
    }
  }

  private static Segment toInsideSegments(Circle larger, Circle smaller, boolean aIsLarger, Vector3 tangentPoint) {
    Vector3 offset = larger.center.minus(tangentPoint).toMagnitude(smaller.radius);
    Vector3 largerTangent = tangentPoint.plus(offset);
    Vector3 smallerTangent = smaller.center.plus(offset);
    return Segment.straight(
        aIsLarger ? largerTangent : smallerTangent,
        aIsLarger ? smallerTangent : largerTangent);
  }

  public static Circle closeTurningRadius(Vector3 point, CarData car) {
    Circles circles =
        turningRadiusCircles(
            car.position,
            Math.max(800, car.groundSpeed),
            car.orientation.getNoseVector());

    if (circles.cw.center.distance(point) < circles.ccw.center.distance(point)) {
      return circles.cw;
    }
    return circles.ccw;
  }

  public static Circles turningRadiusCircles(CarData car) {
    return turningRadiusCircles(car.position, car.groundSpeed, car.orientation.getNoseVector());
  }

  public static Circles turningRadiusCircles(Vector3 position, double speed, Vector3 noseVector) {
    double radius = Circle.radiusForPath(speed);

    Vector2 perpVelocity = noseVector.flatten().clockwisePerpendicular();
    Circle cw = Circle.forPath(position.plus(perpVelocity.asVector3().toMagnitude(radius)), radius);
    Circle ccw = Circle.forPath(position.plus(perpVelocity.asVector3().toMagnitude(-radius)), radius);

    return new Circles(cw, ccw);
  }

  public static class CircleTangents {
    public final Segment cwcw;
    public final Segment ccwcw;
    public final Segment cwccw;
    public final Segment ccwccw;

    public final int numDistinctTangents;

    public CircleTangents(
        Segment cwcw,
        Segment ccwcw,
        Segment cwccw,
        Segment ccwccw) {

      this.cwcw = cwcw;
      this.ccwcw = ccwcw;
      this.cwccw = cwccw;
      this.ccwccw = ccwccw;

      if (Double.isNaN(cwcw.start.x)) {
        numDistinctTangents = 0;
      } else if (Double.isNaN(cwccw.start.x)) {
        numDistinctTangents = 2;
      } else if (ccwcw.equals(cwccw)) {
        numDistinctTangents = 3;
      } else {
        numDistinctTangents = 4;
      }
    }

    public ImmutableList<Segment> getSegments() {
      return ImmutableList.of(cwcw, ccwcw, cwccw, ccwccw);
    }

    public Segment getSegment(Shape shape) {
      switch (shape) {
        case CWCW:
          return cwcw;
        case CCWCW:
          return ccwcw;
        case CWCCW:
          return cwccw;
        case CCWCCW:
          return ccwccw;
      }
      throw new IllegalArgumentException("Unhandled shape " + shape);
    }

    public enum Shape {
      CWCW,
      CCWCW,
      CWCCW,
      CCWCCW;

      public boolean startsClockWise() {
        return this == CWCW || this == CWCCW;
      }

      public boolean endsClockWise() {
        return this == CWCW || this == CCWCW;
      }
    }
  }

  public static class TangentPoints {
    public final Vector3 left; // Clockwise
    public final Vector3 right; // Counter-clockwise

    TangentPoints(Vector3 left, Vector3 right) {
      this.left = left;
      this.right = right;
    }

    public ImmutableList<Vector3> getPoints() {
      return ImmutableList.of(left, right);
    }
  }

  private Paths() {
  }

  public static class Circles {
    public final Circle cw; // Clockwise
    public final Circle ccw; // Counter-clockwise

    Circles(Circle cw, Circle ccw) {
      this.cw = cw;
      this.ccw = ccw;
    }

    public ImmutableList<Circle> getCircles() {
      return ImmutableList.of(cw, ccw);
    }
  }
}
