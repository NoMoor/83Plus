package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
import com.eru.rlbot.bot.maneuver.Flip;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.Controls;

/** Handles flip tactics for front and diagonal flips. */
public class FlipTactician extends Tactician {

  private Flip flip;

  public FlipTactician(ApolloGuidanceComputer bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  @Override
  public boolean isLocked() {
    return !flip.isComplete();
  }

  @Override
  public void internalExecute(DataPacket input, Controls output, Tactic tactic) {
    checkTactic(input, tactic);

    flip.execute(input, output, tactic);
  }

  @Override
  protected void reset(DataPacket input) {
    flip = Flip.builder()
        .setTarget(input.ball.position)
        .build();
  }
}


