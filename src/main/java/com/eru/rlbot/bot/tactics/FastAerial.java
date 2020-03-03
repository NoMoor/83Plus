package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
import com.eru.rlbot.bot.utils.Monitor;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.Controls;

/**
 * Performs a fast aerial.
 */
public class FastAerial extends Tactician {

  private boolean isFinished;
  private int hasJumpedTicks;

  private Monitor monitor;

  FastAerial(ApolloGuidanceComputer bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  // TODO: Measure how much velocity this adds.
  @Override
  void internalExecute(DataPacket input, Controls output, Tactic nextTactic) {
    if (monitor == null) {
      monitor = Monitor.create(input);
    }
    monitor.trackWhile(!isFinished, input.car);

    if (isFinished) {
      reset(input);
    }

    if (input.car.hasWheelContact) {
      output
          .withJump()
          .withPitch(1.0)
          .withBoost();

      // If we haven't taken off yet, give up.
      if (hasJumpedTicks++ > 8) {
        isFinished = true;
      }
    } else if (!JumpManager.forCar(input.car).hasMaxJumpHeight()) {
      output
          .withJump()
          .withBoost()
          .withPitch(1.0);
    } else if (!JumpManager.forCar(input.car).hasReleasedJumpInAir()) {
      output
          .withBoost()
          .withPitch(1.0);
    } else if (JumpManager.forCar(input.car).canFlip()) {
      // Release Pitch for this frame.
      output
          .withJump()
          .withBoost();
    } else {
      output
          .withPitch(1)
          .withBoost();
      isFinished = true;
    }
  }

  @Override
  protected void reset(DataPacket input) {
    isFinished = false;
    hasJumpedTicks = 0;
    monitor = null;
  }

  @Override
  public boolean isLocked() {
    return !isFinished;
  }
}
