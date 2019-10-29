package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

public class RotateTactician extends Tactician {

  RotateTactician(EruBot bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  public static boolean shouldRotateBack(DataPacket input) {
    double carToGoal = input.car.position.distance(Goal.ownGoal(input.car.team).center);
    double ballToGoal = input.ball.position.distance(Goal.ownGoal(input.car.team).center);

    // TODO: Check to see if you are between the ball and the goal.
    return ballToGoal - 500 < carToGoal;
  }

  @Override
  void execute(DataPacket input, ControlsOutput output, Tactic tactic) {
    output.withSteer(Angles.flatCorrectionDirection(input.car, tactic.target.position))
        .withThrottle(1);

    if (input.car.position.distance(tactic.getTarget()) < 500) {
      tacticManager.setTacticComplete(tactic);
    }
  }
}
