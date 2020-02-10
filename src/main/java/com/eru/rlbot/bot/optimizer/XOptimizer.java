package com.eru.rlbot.bot.optimizer;

import com.eru.rlbot.common.input.BoundingBox;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.Range;

public class XOptimizer extends Optimizer {

  private static final double X_PRECISION = .1; // How accurate to get x.
  private static final double X_EPSILON = 150; // Proportionate step size to take
  private static final double X_GAMMA = 15; // How small of an area to evaluate x gradient

  @Override
  int getMaxSteps() {
    return 20;
  }

  @Override
  public Range<Float> getRange() {
    return Range.closed(-BoundingBox.halfWidth - 20, BoundingBox.halfWidth + 20);
  }

  @Override
  public double getEpsilon() {
    return X_EPSILON;
  }

  @Override
  public double getGamma() {
    return X_GAMMA;
  }

  @Override
  public double getPrecision() {
    return X_PRECISION;
  }

  @Override
  public CarData adjustCar(CarData car, double value) {
    Vector3 newPosition = car.position.plus(car.orientation.getLeftVector().toMagnitude(value));

    return car.toBuilder()
        .setPosition(newPosition)
        .build();
  }
}
