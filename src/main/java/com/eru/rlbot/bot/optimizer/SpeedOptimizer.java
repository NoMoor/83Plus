package com.eru.rlbot.bot.optimizer;

import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.Range;

/**
 * Optimizes hitting the ball with different speeds.
 */
public class SpeedOptimizer extends Optimizer {

  private static final double S_PRECISION = 50; // How accurate to get Z.
  private static final double S_EPSILON = 10; // Proportionate step size to take
  private static final double S_GAMMA = 500; // How small of an area to evaluate Z gradient
  private static Range<Float> SPEED = Range.closed(0f, (float) Constants.BOOSTED_MAX_SPEED);
  private final float initialSpeed;

  public SpeedOptimizer(CarData car) {
    initialSpeed = (float) car.groundSpeed;
  }

  @Override
  Range<Float> getRange() {
    return SPEED;
  }

  @Override
  double getEpsilon() {
    return S_EPSILON;
  }

  @Override
  double getGamma() {
    return S_GAMMA;
  }

  @Override
  double getPrecision() {
    return S_PRECISION;
  }

  @Override
  CarData adjust(CarData car, double value) {
    return car.toBuilder()
        .setVelocity(car.velocity.toMagnitude(value))
        .build();
  }

  @Override
  float getInitialValue() {
    return initialSpeed;
  }

  @Override
  public CarData optimize(BallData ball, CarData car, Vector3 target) {
    return super.optimize(ball, car, target);
  }

  @Override
  double score(BallData ballData, Vector3 target) {
    // TODO: Update this. Faster is better. Closer to target height is best.
    return super.score(ballData, target);
  }
}
