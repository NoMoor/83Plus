package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

// TODO: Needs to know where the ball is and where teammates (if any) are.
// Relies on other low level tactical units to do the movement but this tactician is responsible for planning a pass to
// a ready ally.
public class PassTactician extends Tactician {

  PassTactician(EruBot bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  @Override
  public void execute(DataPacket input, ControlsOutput output, Tactic nextTactic) {

  }
}
