package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;

public class Locations {

  public static Vector3 ballToGoalVector(DataPacket input) {
    return Goal.opponentGoal(input.car.team).center.minus(input.ball.position);
  }

  public static Vector3 carToBall(DataPacket input) {
    return input.ball.position.minus(input.car.position);
  }

  public static boolean isOpponentSideOfBall(DataPacket input) {
    Vector2 ballToGoal = ballToGoalVector(input).flatten();
    Vector2 carToBall = carToBall(input).flatten();

    double correctionAngle = carToBall.correctionAngle(ballToGoal);

    return Math.abs(correctionAngle) > Math.PI * .25;
  }

  private Locations() {}
}
