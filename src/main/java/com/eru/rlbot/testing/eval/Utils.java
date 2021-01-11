package com.eru.rlbot.testing.eval;

import com.eru.rlbot.common.ScenarioProtos;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.gamestate.BallState;
import rlbot.gamestate.CarState;
import rlbot.gamestate.PhysicsState;

public class Utils {

  public static String toString(ScenarioProtos.Scenario.BallState ball) {
    return String.format("x:%d y:%d z:%d", (int) ball.getPos(0), (int) ball.getPos(1), (int) ball.getPos(2));
  }

  public static BallState toDesired(ScenarioProtos.Scenario.BallState ball) {
    return new BallState()
        .withPhysics(new PhysicsState()
            .withLocation(Vector3.of(ball.getPos(0), ball.getPos(1), ball.getPos(2)).toDesired())
            .withVelocity(Vector3.of(ball.getVel(0), ball.getVel(1), ball.getVel(2)).toDesired())
            .withAngularVelocity(Vector3.zero().toDesired()));
  }

  public static CarState toDesired(ScenarioProtos.Scenario.CarState car) {
    return new CarState()
        .withPhysics(new PhysicsState()
            .withLocation(Vector3.of(car.getPos(0), car.getPos(1), car.getPos(2)).toDesired())
            .withVelocity(Vector3.of(car.getVel(0), car.getVel(1), car.getVel(2)).toDesired())
            .withAngularVelocity(Vector3.of(car.getSpin(0), car.getSpin(1), car.getSpin(2)).toDesired())
            .withRotation(Orientation.convert(car.getOrientation(0), car.getOrientation(1), car.getOrientation(2)).toEuclidianVector()))
        .withBoostAmount((float) car.getBoost());
  }

  private Utils() {
  }
}
