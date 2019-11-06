package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;

/** Utilities to get angles. */
public final class Angles {

  public static double flatCorrectionDirection(BallData ballData, Vector3 targetPoint) {
    return flatCorrectionAngle(ballData.position, ballData.velocity, targetPoint);
  }

  public static double flatCorrectionDirection(CarData carData, Vector3 targetPoint) {
    return flatCorrectionAngle(carData.position, carData.orientation.getNoseVector(), targetPoint);
  }

  public static double correctionDirection(Vector2 sourcePoint, Vector2 sourceOrientation, Vector2 targetPoint) {
    return flatCorrectionAngle(sourcePoint, sourceOrientation, targetPoint);
  }

  public static double flatCorrectionAngle(Vector3 sourcePoint, Vector3 sourceOrientation, Vector3 targetPoint) {
    return flatCorrectionAngle(sourcePoint.flatten(), sourceOrientation.flatten(), targetPoint.flatten());
  }

  public static double flatCorrectionAngle(Vector2 sourcePoint, Vector2 sourceOrientation, Vector2 targetPoint) {
    // Subtract the two positions to get a vector pointing from the car to the ball.
    Vector2 sourceToTargetVector = targetPoint.minus(sourcePoint);

    return sourceOrientation.correctionAngle(sourceToTargetVector);
  }

  public static double minAbs(double a, double b) {
    return Math.abs(a) < Math.abs(b)
        ? a
        : b;
  }

  private static final double CORRECTION_GAIN = 6d;
  public static Vector3 getTargetOffset(Vector3 carBall, double correctionAngle) {
    double xCorrection = -Constants.BALL_RADIUS * Math.sin(correctionAngle) * CORRECTION_GAIN;
    double yCorrection = -Constants.BALL_RADIUS;

    double rotation = carBall.flatten().correctionAngle(Vector2.NORTH);

    return rotate(new Vector2(xCorrection, yCorrection), rotation).asVector3();
  }

  public static Vector2 rotate(Vector2 position, double angle) {
    double cos = Math.cos(angle);
    double sin = Math.sin(angle);

    double xPrime = (position.x * cos) + (position.y * sin);
    double yPrime = (-position.x * sin) + (position.y * cos);

    return new Vector2(xPrime, yPrime);
  }

  public static Vector3 carBall(DataPacket input) {
    return carTarget(input.car, input.ball.position);
  }

  public static Vector3 carTarget(CarData car, Vector3 target) {
    return target.minus(car.position);
  }

  private Angles() {}
}
