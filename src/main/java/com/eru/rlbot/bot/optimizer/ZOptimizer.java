package com.eru.rlbot.bot.optimizer;

import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.BoundingBox;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.Range;

public class ZOptimizer extends Optimizer {

  private static final double Z_PRECISION = .1; // How accurate to get Z.
  private static final double Z_EPSILON = 10; // Proportionate step size to take
  private static final double Z_GAMMA = 15; // How small of an area to evaluate Z gradient
  private final Range<Float> range;

  ZOptimizer(BallData ball, CarData car) {
    // Do not adjust through walls.
    float floorOffset = Constants.CAR_AT_REST - car.position.z;
    float ballBottomOffset = (ball.position.z - Constants.BALL_RADIUS) - car.position.z;
    float ceilingOffset = (Constants.FIELD_HEIGHT - BoundingBox.height) - car.position.z;
    float ballTopOffset = (ball.position.z + Constants.BALL_RADIUS) - car.position.z;

    range = Range.closed(Math.max(floorOffset, ballBottomOffset), Math.min(ceilingOffset, ballTopOffset));
  }

  @Override
  public Range<Float> getRange() {
    return range;
  }

  @Override
  public double getEpsilon() {
    return Z_EPSILON;
  }

  @Override
  public double getGamma() {
    return Z_GAMMA;
  }

  @Override
  public double getPrecision() {
    return Z_PRECISION;
  }

  @Override
  public CarData adjustCar(CarData car, double value) {
    Vector3 newPosition = car.position.plus(car.orientation.getRoofVector().toMagnitude(value));

    return car.toBuilder()
        .setPosition(newPosition)
        .build();
  }
}
