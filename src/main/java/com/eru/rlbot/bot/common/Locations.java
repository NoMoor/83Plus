package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;

public class Locations {

  public static Vector3 ballToInsideLeftGoal(DataPacket input) {
    return Goal.opponentGoal(input.car.team).leftInside.minus(input.ball.position);
  }

  public static Vector3 ballToInsideRightGoal(DataPacket input) {
    return Goal.opponentGoal(input.car.team).rightInside.minus(input.ball.position);
  }

  public static Vector3 ballToGoalCenter(DataPacket input) {
    return Goal.opponentGoal(input.car.team).center.minus(input.ball.position);
  }

  public static Vector3 carToBall(DataPacket input) {
    return input.ball.position.minus(input.car.position);
  }

  public static boolean isOpponentSideOfBall(DataPacket input) {
    double correctionAngle = getSmallestBallGoalCorrection(input);

    return Math.abs(correctionAngle) > Math.PI * .25;
  }

  public static double getSmallestBallGoalCorrection(DataPacket input) {
    Vector2 ballToGoalRight = ballToInsideRightGoal(input).flatten();
    Vector2 ballToGoalLeft = ballToInsideLeftGoal(input).flatten();

    Vector2 carToBall = carToBall(input).flatten();

    double leftCorrectAngle = ballToGoalLeft.correctionAngle(carToBall);
    double rightCorrectAngle = ballToGoalRight.correctionAngle(carToBall);

    if (leftCorrectAngle > 0 && rightCorrectAngle < 0) {
      return 0;
    }

    return Math.min(leftCorrectAngle, rightCorrectAngle);
  }

  private Locations() {}
}
