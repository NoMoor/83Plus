package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;

/**
 * Represents a circle in the xy coordinate space on the field.
 */
public final class Circle {

  public final Vector3 center;
  public final double radius;
  public final double maxSpeed;
  public double circumference;

  private Circle(Vector3 center, double radius) {
    this.center = center;
    this.radius = radius;
    this.maxSpeed = Constants.maxSpeed(radius);
    this.circumference = Math.PI * radius * 2;
  }

  public static Circle forPath(Vector3 center, double radius) {
    return new Circle(center, radius);
  }

  public static double radiusFromStraight(double speed) {
    return radius(speed);
  }

  public static double radiusForPath(double speed) {
    return radius(speed);
  }

  private static double radius(double velocity) {
    if (velocity <= 0) {
      velocity = 1;
    }

    return (1 / Constants.curvature(velocity));
  }

  public Vector3 pointOnCircle(double radians) {
    return pointOnCircle(center, radius, radians);
  }

  public static Vector2 pointOnCircle(Vector2 center, double radius, double radians) {
    double x = center.x + (radius * Math.cos(radians));
    double y = center.y + (radius * Math.sin(radians));

    return Vector2.of(x, y);
  }

  public static Vector3 pointOnCircle(Vector3 center, double radius, double radians) {
    double x = center.x + (radius * Math.cos(radians));
    double y = center.y + (radius * Math.sin(radians));

    return Vector3.of(x, y, center.z);
  }

  public static Circle fromMoment(Moment moment) {
    double radius;
    switch (moment.type) {
      case BALL:
        radius = Constants.BALL_COLLISION_RADIUS;
        break;
      case CAR:
        radius = Constants.CAR_WIDTH / 2;
        break;
      case SMALL_BOOST:
        radius = Constants.SMALL_BOOST_PICKUP_RADIUS;
        break;
      case WAY_POINT:
      case LARGE_BOOST:
      default:
        radius = Constants.LARGE_BOOST_PICKUP_RADIUS;
    }
    return new Circle(moment.position.flat(), radius);
  }

  public boolean isClockwise(CarData car) {
    return isClockwise(car.position, car.orientation.getNoseVector());
  }

  private boolean isClockwise(Vector3 position, Vector3 noseVector) {
    return center.minus(position).cross(noseVector).z < 0;
  }

  public double radianOffset(Vector3 point) {
    Vector2 ray = point.minus(center).flatten();
    return Vector2.WEST.correctionAngle(ray);
  }

  public ImmutableList<Vector3> intersections(Circle circle) {
    Vector3 circleToCircle = circle.center.flat().minus(this.center.flat());
    double distance = circleToCircle.magnitude();
    if (distance > circle.radius + this.radius || distance < Math.abs(this.radius - circle.radius)) {
      return ImmutableList.of();
    }

    // Distance from the center of this circle to the point in-between the overlap of the circles.
    double centerToMidoverlap =
        ((this.radius * this.radius) - (circle.radius * circle.radius) + (distance * distance)) / (2 * distance);
    Vector3 midPoint = center.plus(circleToCircle.toMagnitude(centerToMidoverlap));

    // Circles touch at one point.
    if (centerToMidoverlap * 2 == distance) {
      return ImmutableList.of(midPoint);
    }

    double verticalOffset = Math.sqrt((radius * radius) - (centerToMidoverlap * centerToMidoverlap));

    Vector3 perpendicular = circleToCircle.counterClockwisePerpendicular().normalize();
    Vector3 p1 = midPoint.plus(perpendicular.toMagnitude(verticalOffset));
    Vector3 p2 = midPoint.plus(perpendicular.toMagnitude(-verticalOffset));

    return ImmutableList.of(p1, p2);
  }
}
