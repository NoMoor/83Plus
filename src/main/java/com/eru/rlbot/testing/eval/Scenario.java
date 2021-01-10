package com.eru.rlbot.testing.eval;

import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;

public class Scenario {

  private final String name;
  private final BallData ball;
  private final CarData car;

  public Scenario(String name, BallData ball, CarData car) {
    this.name = name;
    this.ball = ball;
    this.car = car;
  }

  public String getName() {
    return name;
  }

  public BallData getBall() {
    return ball;
  }

  public CarData getCar() {
    return car;
  }
}
