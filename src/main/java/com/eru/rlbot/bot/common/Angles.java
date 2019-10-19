package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;

/** Utilities to get angles. */
public final class Angles {

  public static double flatCorrectionDirection(BallData ballData, Vector3 targetPoint) {
    return -flatCorrectionAngle(ballData.position, ballData.velocity, targetPoint);
  }

  public static double flatCorrectionDirection(CarData carData, Vector3 targetPoint) {
    return -flatCorrectionAngle(carData.position, carData.orientation.noseVector, targetPoint);
  }

  public static double correctionDirection(Vector2 sourcePoint, Vector2 sourceOrientation, Vector2 targetPoint) {
    return -flatCorrectionAngle(sourcePoint, sourceOrientation, targetPoint);
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

  private Angles() {}
}
