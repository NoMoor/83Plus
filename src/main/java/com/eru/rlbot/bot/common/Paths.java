package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.vector.Vector3;

public class Paths {

  public static Circle turnRadius(CarData car, Vector3 targetLocation) {
    Vector3 front = car.orientation.getNoseVector().normalize();
    Vector3 up = front.cross(Vector3.of(0, 0, 1).cross(front)).normalize();
    Vector3 left = up.cross(front).normalize();

    double turnRadius = Constants.radius(car.groundSpeed);

    Vector3 centerOffset = left.toMagnitude(turnRadius);

    Vector3 center1 = car.position.plus(centerOffset);
    Vector3 center2 = car.position.minus(centerOffset);
    return new Circle(center1.distance(targetLocation) < center2.distance(targetLocation) ? center1 : center2, turnRadius, car.groundSpeed);
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
        turnRadius,
        target.groundSpeed);
  }

  static Vector3 tangent(Circle circle, Vector3 currentCarPos, CarData targetCar) {
    // Find a circle that goes through both the center of the original circle and the given position.
    Vector3 bisector = circle.center.minus(currentCarPos).divide(2);

    double d = bisector.magnitude();
    double r = circle.radius;
    double R = d; // The larger circle goes through the smaller.

    double x = ((d * d) - (r * r) + (R * R)) / (2 * d);
    double a = (1 / (2 * d)) * Math.sqrt((4 * (d * d) * (R * R)) - Math.pow((d * d) - (r * r) + (R * R), 2));

    Vector3 tanx = currentCarPos.plus(bisector.plus(bisector.normalize().toMagnitude(x)));
    Vector3 perp = bisector.flatten().perpendicular().scaledToMagnitude(a).asVector3();

    Vector3 tangent1 = tanx.plus(perp);
    Vector3 tangent2 = tanx.minus(perp);

    // Select the point going in the right direction.
    Vector3 centerToTarget = circle.center.minus(targetCar.position);
    double targetHandedness = Math.signum(centerToTarget.cross(targetCar.orientation.getNoseVector()).z);

    Vector3 centerToTan1 = circle.center.minus(tangent1);
    double tan1Handedness = Math.signum(centerToTan1.cross(tangent1.minus(currentCarPos)).z);

    return targetHandedness == tan1Handedness ? tangent1 : tangent2;
  }

  private Paths() {
  }
}
