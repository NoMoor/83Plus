package com.eru.rlbot.bot.optimizer;

import com.eru.rlbot.common.input.CarData;

public class OptimizationResult {
  public final CarData car;
  public final double xOffset;
  public final double speedDiff;
  public final double zOffset;
  public final double aOffset;

  public OptimizationResult(CarData optimalCar, double xOffset, double zOffset, double aOffset, double speedDiff) {
    this.car = optimalCar;
    this.xOffset = xOffset;
    this.zOffset = zOffset;
    this.aOffset = aOffset;
    this.speedDiff = speedDiff;
  }

  public static OptimizationResult create(CarData car, double xOffset, double zOffset, double aOffset, double speedDiff) {
    return new OptimizationResult(car, xOffset, zOffset, aOffset, speedDiff);
  }
}
