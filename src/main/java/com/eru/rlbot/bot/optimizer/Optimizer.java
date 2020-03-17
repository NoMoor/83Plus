package com.eru.rlbot.bot.optimizer;

import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.prediction.CarBallCollision;
import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.Numbers;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.Range;

/**
 * An abstract class which can be extended to provide optimization in a particular direction.
 */
public abstract class Optimizer {

  protected float currentValue = getInitialValue();
  private int steps = 0;
  private boolean isDone = false;

  abstract Range<Float> getRange();

  abstract double getEpsilon();

  abstract double getGamma();

  abstract double getPrecision();

  abstract CarData adjust(CarData car, double value);

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

    return adjust(car, currentValue);
  }

  public void doStep(BallData ball, CarData car, Vector3 target) {
    float prevValue = currentValue;
    double gradient = getGradient(ball, target, car, currentValue);
    currentValue -= getEpsilon() * gradient;
    double stepSize = Math.abs(currentValue - prevValue);

    if (steps++ > getMaxSteps() || stepSize < getPrecision() || !getRange().contains(currentValue)) {
      isDone = true;
    }

    currentValue = Numbers.clamp(currentValue, getRange().lowerEndpoint(), getRange().upperEndpoint());
  }

  public void doStep(Moment moment, CarData car, Vector3 target) {

  }

  protected double getGradient(BallData ball, Vector3 target, CarData car, double currentValue) {
    BallData resultA = CarBallCollision.calculateCollision(ball, adjust(car, currentValue));
    double aScore = score(resultA, target);

    BallData resultB = CarBallCollision.calculateCollision(ball, adjust(car, currentValue + getGamma()));
    double bScore = score(resultB, target);

    return (bScore - aScore) / getGamma();
  }

  double score(BallData ballData, Vector3 target) {
    Vector3 ballTarget = target.minus(ballData.position);

    double flatAngleOffset = ballTarget.flatten().correctionAngle(ballData.velocity.flatten());

    double flatDistance = ballTarget.magnitude();
    double groundSpeed = ballData.velocity.flatten().magnitude();
    double timeToTarget = flatDistance / groundSpeed;

    double verticalAngleOffset = 0;
//    double verticalAngleOffset = 10_000; // Arbitrarily large number
//    if (groundSpeed != 0) {
//      double heightOffset = heightOffset(ballData.velocity.z, timeToTarget);
//      verticalAngleOffset = Math.atan((ballTarget.z - heightOffset) / ballTarget.magnitude());
//    }

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
