package com.eru.rlbot.bot.ballchaser.v1.strats;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.ballchaser.v1.tactics.RotateTactician;
import com.eru.rlbot.bot.ballchaser.v1.tactics.Tactic;
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
    return ballIsNearGoal(input);
  }

  private static boolean ballIsNearGoal(DataPacket input) {
    Optional<BallPrediction> ballPredictionOptional = DllHelper.getBallPrediction();
    if (!ballPredictionOptional.isPresent()) {
      return false;
    }

    final BallPrediction ballPrediction = ballPredictionOptional.get();

    int i = 0;
    while (i < ballPrediction.slicesLength()) {
      Physics ballPysics = ballPrediction.slices(i).physics();

      double distanceToGoal =
          Vector3.of(ballPysics.location()).distance(Goal.ownGoal(input.car.team).center);

      boolean ballMovingTowardsGoal = input.car.team == 0
          ? ballPysics.velocity().y() < 0
          : ballPysics.velocity().y() > 0;

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
      tacticManager.setTactic(new Tactic(Goal.ownGoal(bot.team).center, Tactic.Type.ROTATE));
    } else {
      tacticManager.setTactic(new Tactic(Goal.ownGoal(bot.team).center, Tactic.Type.DEFEND));
    }

    return true;
  }

  @Override
  public Strategy.Type getType() {
    return Strategy.Type.DEFEND;
  }
}
