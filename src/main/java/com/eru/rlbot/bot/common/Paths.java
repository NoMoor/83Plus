package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.vector.Vector3;

public class Paths {

  public static Circle turnRadius(CarData car, Vector3 targetLocation) {
    Vector3 front = car.orientation.getNoseVector().normalized();
    Vector3 up = front.cross(Vector3.of(0, 0, 1).cross(front)).normalized();
    Vector3 left = up.cross(front).normalized();

    double turnRadius = Constants.radius(car.groundSpeed);

    Vector3 centerOffset = left.toMagnitude(turnRadius);

    Vector3 center1 = car.position.plus(centerOffset);
    Vector3 center2 = car.position.minus(centerOffset);
    return new Circle(center1.distance(targetLocation) < center2.distance(targetLocation) ? center1 : center2, turnRadius);
  }

  public static Circle closeApproach(CarData car, CarData currentCarData) {
    Vector3 currentLocation = currentCarData.position;
    Vector3 front = car.orientation.getNoseVector().normalized();
    Vector3 up = front.cross(Vector3.of(0, 0, 1).cross(front)).normalized();
    Vector3 left = up.cross(front).normalized();

    double turnRadius = Constants.radius(currentCarData.groundSpeed) * 1.4;

    Vector3 centerOffset = left.toMagnitude(turnRadius);

    Vector3 center1 = car.position.plus(centerOffset);
    Vector3 center2 = car.position.minus(centerOffset);
    return new Circle(center1.distance(currentLocation) < center2.distance(currentLocation) ? center1 : center2, turnRadius);
  }

  public static Vector3 tangent(Circle circle, Vector3 position, Vector3 targetPosition) {
    // Find a circle that goes through both the center of the original circle and the given position.
    Vector3 bisector = circle.center.minus(position).divide(2);

    double d = bisector.norm();
    double r = circle.radius;
    double R = d; // The larger circle goes through the smaller.

    double x = ((d * d) - (r * r) + (R * R)) / (2 * d);
    double a = (1 / (2 * d)) * Math.sqrt((4 * (d * d) * (R * R)) - Math.pow((d * d) - (r * r) + (R * R), 2));

    Vector3 tanx = position.plus(bisector.plus(bisector.normalized().toMagnitude(x)));
    Vector3 perp = bisector.flatten().perpendicular().scaledToMagnitude(a).asVector3();

    Vector3 tangent1 = tanx.plus(perp);
    Vector3 tangent2 = tanx.minus(perp);

    return tangent1.distance(targetPosition) < tangent2.distance(targetPosition) ? tangent1 : tangent2;
  }

  private Paths() {
  }
}
