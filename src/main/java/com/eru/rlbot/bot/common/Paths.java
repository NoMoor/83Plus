package com.eru.rlbot.bot.common;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;

public class Paths {

  public static Circle turnRadius(CarData car, Vector3 targetLocation) {
    Vector3 front = car.orientation.getNoseVector().normalize();
    Vector3 up = front.cross(Vector3.of(0, 0, 1).cross(front)).normalize();
    Vector3 left = up.cross(front).normalize();

    double turnRadius = Constants.radius(car.groundSpeed);

    Vector3 centerOffset = left.toMagnitude(turnRadius);

    Vector3 center1 = car.position.plus(centerOffset);
    Vector3 center2 = car.position.minus(centerOffset);
    return new Circle(center1.distance(targetLocation) < center2.distance(targetLocation) ? center1 : center2, turnRadius);
  }

  /**
   * Returns the ideal approach based on the target speed.
   */
  static Circle closeApproach(CarData target, CarData currentCarData) {
    Vector3 currentLocation = currentCarData.position;
    Vector3 front = target.orientation.getNoseVector().normalize();
    Vector3 up = front.cross(Vector3.of(0, 0, 1).cross(front)).normalize();
    Vector3 left = up.cross(front).normalize();

    double turnRadius = Constants.radius(target.groundSpeed) * 1.4;

    Vector3 centerOffset = left.toMagnitude(turnRadius);

    Vector3 center1 = target.position.plus(centerOffset);
    Vector3 center2 = target.position.minus(centerOffset);

    return new Circle(center1.distance(currentLocation) < center2.distance(currentLocation)
        ? center1
        : center2,
        turnRadius);
  }

  static Vector3 tangentForTargetDirection(Circle circle, Vector3 currentCarPos, CarData targetCar) {
    TangentPoints tangentPoints = tangents(circle, currentCarPos);

    // Select the point going in the right direction.
    Vector3 centerToTarget = circle.center.minus(targetCar.position);
    double targetHandedness = Math.signum(centerToTarget.cross(targetCar.orientation.getNoseVector()).z);

    return targetHandedness < 0 ? tangentPoints.left : tangentPoints.right;
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
    Vector3 perp = bisector.flatten().perpendicular().scaledToMagnitude(a).asVector3();

    // TODO: Label these. One should always have the same spin.
    return new TangentPoints(tanx.minus(perp), tanx.plus(perp));
  }

  public static CircleTangents tangents(Circle a, Circle b) {
    if (a.radius == b.radius) {
      a = new Circle(a.center, a.radius + 1);
    }
    Circle larger = a.radius > b.radius ? a : b;
    Circle smaller = larger == a ? b : a;
    boolean aIsLarger = a.radius > b.radius;

    // TODO: Do checking to see if they overlap.

    // Get inside tangents.
    ImmutableList<Segment> insideSegments =
        tangents(new Circle(larger.center, larger.radius + smaller.radius), smaller.center).getPoints().stream()
            .map(tangentPoint -> toInsideSegments(larger, smaller, aIsLarger, tangentPoint))
            .collect(toImmutableList());

    // Get outside tangents.
    ImmutableList<Segment> outsideSegments =
        tangents(new Circle(larger.center, larger.radius - smaller.radius), smaller.center).getPoints().stream()
            .map(tangentPoint -> toOutsideSegments(larger, smaller, aIsLarger, tangentPoint))
            .collect(toImmutableList());

    return new CircleTangents(
        outsideSegments.get(0),
        insideSegments.get(0),
        insideSegments.get(1),
        outsideSegments.get(1));
  }

  private static Segment toOutsideSegments(Circle larger, Circle smaller, boolean aIsLarger, Vector3 tangentPoint) {
    Vector3 offset = larger.center.minus(tangentPoint).toMagnitude(smaller.radius);
    Vector3 largerTangent = tangentPoint.minus(offset);
    Vector3 smallerTangent = smaller.center.minus(offset);
    return Segment.straight(
        aIsLarger ? largerTangent : smallerTangent,
        aIsLarger ? smallerTangent : largerTangent);
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
    ImmutableList<Circle> circles =
        turningRadiusCircles(
            car.position,
            Math.max(800, car.groundSpeed),
            car.orientation.getNoseVector());

    Circle left = circles.get(0);
    Circle right = circles.get(1);

    if (left.center.distance(point) < right.center.distance(point)) {
      return left;
    }
    return right;
  }

  public static ImmutableList<Circle> turningRadiusCircles(CarData car) {
    return turningRadiusCircles(car.position, car.groundSpeed, car.orientation.getNoseVector());
  }

  public static ImmutableList<Circle> turningRadiusCircles(Vector3 position, double speed, Vector3 noseVector) {
    double radius = Constants.radius(speed);

    Vector2 perpVelocity = noseVector.flatten().perpendicular();
    Circle rightCircle = new Circle(position.plus(perpVelocity.asVector3().toMagnitude(radius)), radius);
    Circle leftCircle = new Circle(position.plus(perpVelocity.asVector3().toMagnitude(-radius)), radius);

    return ImmutableList.of(leftCircle, rightCircle);
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
}
