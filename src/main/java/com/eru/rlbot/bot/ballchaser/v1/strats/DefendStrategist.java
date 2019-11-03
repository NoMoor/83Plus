package com.eru.rlbot.bot.ballchaser.v1.strats;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.ballchaser.v1.tactics.RotateTactician;
import com.eru.rlbot.bot.ballchaser.v1.tactics.Tactic;
import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.DllHelper;
import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.flat.BallPrediction;
import rlbot.flat.Physics;

import java.util.Optional;

/** Responsible for shadowing, blocking, shots, and clearing. */
public class DefendStrategist extends Strategist {

  DefendStrategist(EruBot bot) {
    super(bot);
  }

  static boolean shouldDefend(DataPacket input) {
    return ballIsNearGoal(input) || ballIsFarCorner(input);
  }

  private static boolean ballIsFarCorner(DataPacket input) {
    Vector3 oppGoalCenter = Goal.opponentGoal(input.car.team).center;
    Vector3 ballPosition = input.ball.position;

    Vector3 ballToGoal = oppGoalCenter.minus(ballPosition);

    return Math.abs(ballToGoal.x) > Math.abs(ballToGoal.y);
  }

  private static boolean ballIsNearGoal(DataPacket input) {
    Optional<BallPrediction> ballPredictionOptional = DllHelper.getBallPrediction();
    if (!ballPredictionOptional.isPresent()) {
      return false;
    }

    final BallPrediction ballPrediction = ballPredictionOptional.get();

    int i = 0;
    while (i < ballPrediction.slicesLength()) {
      Physics ballPhysics = ballPrediction.slices(i).physics();

      double distanceToGoal =
          Vector3.of(ballPhysics.location()).distance(Goal.ownGoal(input.car.team).center);

      boolean ballMovingTowardsGoal = input.car.team == 0
          ? ballPhysics.velocity().y() < 0
          : ballPhysics.velocity().y() > 0;

      if (distanceToGoal < 5000 && ballMovingTowardsGoal) {
        return true;
      }

      i += 10;
    }

    return false;
  }

  @Override
  public boolean assign(DataPacket input) {
    if (RotateTactician.shouldRotateBack(input)) {
      // Rotate back between the ball and the goal

      Vector3 ballToGoal = Goal.ownGoal(bot.team).center.minus(input.ball.position);
      double rotationNorm = Math.min(ballToGoal.flatten().norm(), 1500);
      Vector3 rotationTarget = input.ball.position.plus(ballToGoal.scaledToMagnitude(rotationNorm));

      tacticManager.setTactic(new Tactic(rotationTarget, Tactic.Type.ROTATE));
    } else {
      tacticManager.setTactic(new Tactic(input.car.position, Tactic.Type.DEFEND));
    }

    return true;
  }

  @Override
  public Strategy.Type getType() {
    return Strategy.Type.DEFEND;
  }
}
