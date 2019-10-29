package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

public abstract class Tactician {

  protected final EruBot bot;
  protected final TacticManager tacticManager;

  Tactician(EruBot bot, TacticManager tacticManager) {
    this.bot = bot;
    this.tacticManager = tacticManager;
  }

  abstract void execute(DataPacket input, ControlsOutput output, Tactic nextTactic);

  public boolean isLocked() {
    return false;
  }
}
