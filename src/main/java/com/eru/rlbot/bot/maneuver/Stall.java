package com.eru.rlbot.bot.maneuver;

import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.ControlsOutput;

public class Stall extends Maneuver {

  private boolean done;

  private Stall() {
  }

  public static Stall create() {
    return new Stall();
  }

  @Override
  public void execute(DataPacket input, ControlsOutput output, Tactic tactic) {
    JumpManager jumpManager = JumpManager.forCar(input.car);
    if (!jumpManager.hasReleasedJumpInAir()) {
      // Release jump for a tick.
    } else if (jumpManager.canFlip()) {
      output
          .withJump()
          .withYaw(-1)
          .withRoll(1);
      done = true;
    }
  }

  @Override
  public boolean isComplete() {
    return done;
  }
}
