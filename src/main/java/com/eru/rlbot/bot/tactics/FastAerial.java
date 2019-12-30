package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.main.Agc;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.ControlsOutput;

public class FastAerial extends Tactician {

  private boolean isFinished;
  private int hasJumpedTicks;

  FastAerial(Agc bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  // TODO: Measure how much velocity this adds.
  @Override
  void execute(DataPacket input, ControlsOutput output, Tactic nextTactic) {
    if (input.car.hasWheelContact) {
      output
          .withJump()
          .withPitch(1.0)
          .withBoost();
      hasJumpedTicks++;

      // If we haven't taken off yet, give up.
      if (hasJumpedTicks > 5) {
        isFinished = true;
      }
    } else if (!JumpManager.hasMaxJumpHeight()) {
      output
          .withJump()
          .withBoost()
          .withPitch(1.0);
    } else if (!JumpManager.hasReleasedJumpInAir()) {
      output
          .withBoost()
          .withPitch(1.0);
    } else if (JumpManager.canFlip()) {
      // Release Pitch for this frame.
      output
          .withJump()
          .withBoost();
      isFinished = true;
    }
  }

  @Override
  public boolean isLocked() {
    return !isFinished;
  }
}
