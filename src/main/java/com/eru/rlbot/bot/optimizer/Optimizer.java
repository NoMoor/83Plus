package com.eru.rlbot.bot.optimizer;

import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.prediction.CarBallCollision;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.Range;

public abstract class Optimizer {

  protected float currentValue = getInitialValue();
  private int steps = 0;
  private boolean isDone = false;

  abstract Range<Float> getRange();

  abstract double getEpsilon();

  abstract double getGamma();

  abstract double getPrecision();

  abstract CarData adjustCar(CarData car, double value);

  float getInitialValue() {
    return 0;
  }

  int getMaxSteps() {
    return 20;
  }

  public final boolean isDone() {
    return isDone;
  }

  public CarData optimize(BallData ball, CarData car, Vector3 target) {
    while (!isDone) {
      doStep(ball, car, target);
    }

    return adjustCar(car, currentValue);
  }

  public void doStep(BallData ball, CarData car, Vector3 target) {
    float prevValue = currentValue;
    double gradient = getGradient(ball, target, car, currentValue);
    currentValue -= getEpsilon() * gradient;
    double stepSize = Math.abs(currentValue - prevValue);

    if (steps++ > getMaxSteps() || stepSize < getPrecision() || !getRange().contains(currentValue)) {
      isDone = true;
    }

    currentValue = Angles3.clip(currentValue, getRange().lowerEndpoint(), getRange().upperEndpoint());
  }

  protected double getGradient(BallData ball, Vector3 target, CarData car, double currentValue) {
    BallData resultA = CarBallCollision.calculateCollision(ball, adjustCar(car, currentValue));
    double aScore = score(resultA, target);

    BallData resultB = CarBallCollision.calculateCollision(ball, adjustCar(car, currentValue + getGamma()));
    double bScore = score(resultB, target);

    return (bScore - aScore) / getGamma();
  }

  double score(BallData ballData, Vector3 target) {
    Vector3 ballTarget = target.minus(ballData.position);

    double flatAngleOffset = ballTarget.flatten().correctionAngle(ballData.velocity.flatten());

    double flatDistance = ballTarget.magnitude();
    double groundSpeed = ballData.velocity.flatten().norm();
    double timeToTarget = flatDistance / groundSpeed;

    double verticalAngleOffset = 10_000; // Arbitrarily large number
    if (groundSpeed != 0) {
      double heightOffset = heightOffset(ballData.velocity.z, timeToTarget);
      verticalAngleOffset = Math.atan((ballTarget.z - heightOffset) / ballTarget.magnitude());
    }

    return Math.abs(flatAngleOffset) + Math.abs(verticalAngleOffset);
  }

  private static double heightOffset(double zVelocity, double time) {
    double position = 0;
    while (time > 0) {
      time -= Constants.STEP_SIZE;
      position += zVelocity;
      zVelocity -= Constants.GRAVITY * Constants.STEP_SIZE;
    }
    return position;
  }
}
