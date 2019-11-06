package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.ControlsOutput;

public class RotateTactician extends Tactician {

  private boolean flipLock;

  RotateTactician(EruBot bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  public static boolean shouldRotateBack(DataPacket input) {
    double carToGoal = input.car.position.distance(Goal.ownGoal(input.car.team).center);
    double ballToGoal = input.ball.position.distance(Goal.ownGoal(input.car.team).center);

    return ballToGoal < carToGoal;
  }

  @Override
  void execute(DataPacket input, ControlsOutput output, Tactic tactic) {
    // TODO: We should be facing the ball when we get to the rotation point.
    double correctionAngle = Angles.flatCorrectionDirection(input.car, tactic.target.position);

    if (flipLock) {
      if (!JumpManager.hasReleasedJumpInAir()) {
        output.withBoost();
      } else {
        flipLock = false;
        output
            .withYaw(correctionAngle)
            .withPitch(-1)
            .withJump();
      }
    } else {
      output.withSteer(correctionAngle)
          .withThrottle(1);

      double distanceToTarget = input.car.position.distance(tactic.getTarget());

      if (distanceToTarget < 100) {
        tacticManager.setTacticComplete(tactic);
      } else if (distanceToTarget > 1000 && correctionAngle < .5) {
        flipLock = true;
        output.withJump();
      }
    }
  }
}
