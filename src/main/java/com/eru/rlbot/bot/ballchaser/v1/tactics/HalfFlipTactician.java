package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

/** Manages straight and back half flips. */
public class HalfFlipTactician extends Tactician {

  HalfFlipTactician(EruBot bot) {
    super(bot);
  }

  @Override
  public boolean execute(DataPacket input, ControlsOutput output, Tactic nextTactic) {
    return false;
  }
}
