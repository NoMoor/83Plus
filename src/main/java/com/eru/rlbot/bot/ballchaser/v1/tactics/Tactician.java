package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

public abstract class Tactician {

  protected final EruBot bot;

  Tactician(EruBot bot) {
    this.bot = bot;
  }

  abstract boolean execute(DataPacket input, ControlsOutput output, Tactic nextTactic);
}
