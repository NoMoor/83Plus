package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;

/** Utilities to get angles. */
public final class Angles {

  public static double flatCorrectionAngle(BallData ball, Vector3 targetPoint) {
    return flatCorrectionAngle(ball.position, ball.velocity, targetPoint);
  }

  public static double flatCorrectionAngle(CarData car, Vector3 targetPoint) {
    return flatCorrectionAngle(car.position, car.orientation.getNoseVector(), targetPoint);
  }

  public static double flatCorrectionAngle(Vector3 sourcePoint, Vector3 sourceOrientation, Vector3 targetPoint) {
    return flatCorrectionAngle(sourcePoint.flatten(), sourceOrientation.flatten(), targetPoint.flatten());
  }

  public static double flatCorrectionAngle(Vector2 sourcePoint, Vector2 targetPoint, Orientation orientation) {
    return flatCorrectionAngle(sourcePoint, orientation.getNoseVector().flatten(), targetPoint);
  }

  public static double flatCorrectionAngle(Vector2 sourcePoint, Vector2 sourceOrientation, Vector2 targetPoint) {
    // Subtract the two positions to get a vector pointing from the car to the ball.
    Vector2 sourceToTargetVector = targetPoint.minus(sourcePoint);

    return sourceOrientation.correctionAngle(sourceToTargetVector);
  }

  public static double correctionAngle(Vector2 sourcePoint, Vector2 sourceOrientation, Vector2 targetPoint) {
    return flatCorrectionAngle(sourcePoint, sourceOrientation, targetPoint);
  }

  public static double minAbs(double a, double b) {
    return Math.abs(a) < Math.abs(b)
        ? a
        : b;
  }

  public static Vector2 rotate(Vector2 position, double angle) {
    double cos = Math.cos(angle);
    double sin = Math.sin(angle);

    double xPrime = (position.x * cos) + (position.y * sin);
    double yPrime = (-position.x * sin) + (position.y * cos);

    return Vector2.of(xPrime, yPrime);
  }

  public static Vector3 carBall(DataPacket input) {
    return carTarget(input.car, input.ball.position);
  }

  public static Vector3 carTarget(CarData car, Vector3 target) {
    return target.minus(car.position);
  }

  private Angles() {}

  public static boolean isRotatingBack(DataPacket input) {
    Vector3 ownGoal = Goal.ownGoal(input.car.team).center;
    double correction = Angles.flatCorrectionAngle(input.car, ownGoal);

    return Math.abs(correction) < Math.PI / 4;
  }

  public static double flatCorrectionAngle(Vector3 current, Vector3 ideal) {
    return current.flatten().correctionAngle(ideal.flatten());
  }

  public static boolean isInFrontOfCar(CarData car, Vector3 targetPosition) {
    return targetPosition.minus(car.position).dot(car.orientation.getNoseVector()) > 0;
  }
}
