package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.main.Acg;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

public abstract class Tactician {

  protected final Acg bot;
  protected final TacticManager tacticManager;

  Tactician(Acg bot, TacticManager tacticManager) {
    this.bot = bot;
    this.tacticManager = tacticManager;
  }

  abstract void execute(DataPacket input, ControlsOutput output, Tactic nextTactic);

  public boolean isLocked() {
    return false;
  }
}
