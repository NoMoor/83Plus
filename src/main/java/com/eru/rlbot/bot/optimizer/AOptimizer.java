package com.eru.rlbot.bot.optimizer;

import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.common.Matrix3;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.Range;

/**
 * Optimizes angle around the equator of the ball.
 */
public class AOptimizer extends Optimizer {

  private static final double A_PRECISION = .025f;
  private static final double A_EPSILON = .1f;
  private static final double A_GAMMA = .1f;
  private static final Range<Float> RANGE = Range.closed((float) -Math.PI, (float) Math.PI);
  private final BallData ball;

  public AOptimizer(BallData ball) {
    this.ball = ball;
  }

  @Override
  public Range<Float> getRange() {
    return RANGE;
  }

  @Override
  public double getEpsilon() {
    return A_EPSILON;
  }

  @Override
  public double getGamma() {
    return A_GAMMA;
  }

  @Override
  public double getPrecision() {
    return A_PRECISION;
  }

  @Override
  public CarData adjust(CarData car, double value) {
    Matrix3 rotate = Angles3.rotationMatrix(value);

    Orientation newCarOrientation =
        Orientation.fromOrientationMatrix(rotate.dot(car.orientation.getOrientationMatrix()));

    Vector3 carBall = ball.position.minus(car.position);
    Vector3 rotatedCarBall = rotate.dot(carBall);

    return car.toBuilder()
        .setOrientation(newCarOrientation)
        .setPosition(car.position.plus(carBall).minus(rotatedCarBall))
        .setVelocity(rotate.dot(car.velocity))
        .build();
  }

  @Override
  double score(BallData ballData, Vector3 target) {
    Vector3 ballTarget = target.minus(ballData.position);

    double flatAngleOffset = ballTarget.flatten().correctionAngle(ballData.velocity.flatten());

    return Math.abs(flatAngleOffset);
  }
}
