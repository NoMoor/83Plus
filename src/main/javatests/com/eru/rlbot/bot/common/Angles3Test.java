package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.CarOrientation;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;

public class Angles3Test {

  public static void main(String[] args) {
    test();
  }

  public static void test() {
    CarData.Builder cBuidler = new CarData.Builder();

    cBuidler.time = 0.0f;
    cBuidler.position = Vector3.of(0, 0, 500);
    cBuidler.velocity = Vector3.of(0, 0, 0);
    cBuidler.angularVelocity = Vector3.of(0.0, 0, 0);
    cBuidler.orientation = CarOrientation.convert(0, 1, 0);
    CarData car = cBuidler.build();

    Matrix3 target = Matrix3.of(Vector3.of(-1, 0, 0), Vector3.of(0, 1, 0), Vector3.of(0, 0, 1));

    ControlsOutput output = new ControlsOutput();

    Angles3.makeControlsFor(car, target, output);

//    turn.step(0.0166);
    print(output.getRoll());
    print(output.getPitch());
    print(output.getYaw());

//    simulation = turn.simulate();
//    print(simulation.time);
  }

  public static void print(float v) {
    System.out.println(v);
  }
}