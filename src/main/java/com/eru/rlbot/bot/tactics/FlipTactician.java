package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.main.Agc;
import com.eru.rlbot.bot.maneuver.FlipHelper;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

/** Handles flip tactics for front and diagonal flips. */
public class FlipTactician extends Tactician {

  private FlipHelper flipHelper;

  public FlipTactician(Agc bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  @Override
  public boolean isLocked() {
    return !flipHelper.isComplete();
  }

  @Override
  public void internalExecute(DataPacket input, ControlsOutput output, Tactic tactic) {
    checkTactic(input, tactic);

    flipHelper.execute(input, output, tactic);
  }

  @Override
  protected void reset(DataPacket input) {
    flipHelper = FlipHelper.builder(bot).build();
  }
}


