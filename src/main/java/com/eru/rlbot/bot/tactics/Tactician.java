package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.main.Agc;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

public abstract class Tactician {

  protected final Agc bot;
  protected final TacticManager tacticManager;

  Tactician(Agc bot, TacticManager tacticManager) {
    this.bot = bot;
    this.tacticManager = tacticManager;
  }

  abstract void execute(DataPacket input, ControlsOutput output, Tactic nextTactic);

  public boolean isLocked() {
    return false;
  }
}
