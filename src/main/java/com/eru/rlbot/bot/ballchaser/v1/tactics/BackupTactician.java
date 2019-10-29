package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

public class BackupTactician extends Tactician {

  private boolean sliding;

  BackupTactician(EruBot bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  @Override
  public void execute(DataPacket input, ControlsOutput output, Tactic tactic) {
    // Stay between the ball and the goal.
    double correctionAngle = Angles.flatCorrectionDirection(input.car, input.ball.position);

    output
        .withSteer(correctionAngle)
        .withThrottle(1.0f);

    if (Math.abs(correctionAngle) > 1.5) {
      output.withSlide();
      sliding = true;
    } else if (sliding && Math.abs(correctionAngle) > 1.0) {
      output.withSlide()
          .withBoost();
    } else {
      sliding = false;
    }
  }
}
