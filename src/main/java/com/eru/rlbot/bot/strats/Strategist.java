package com.eru.rlbot.bot.strats;

import com.eru.rlbot.bot.main.Acg;
import com.eru.rlbot.bot.tactics.TacticManager;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

public abstract class Strategist {

  protected final Acg bot;
  protected final TacticManager tacticManager;

  protected Strategist(Acg bot) {
    this.bot = bot;
    this.tacticManager = new TacticManager(bot);
  }

  /** Assigns control to the given strategist. If the strategist is unable to accept control, it can return false. */
  abstract boolean assign(DataPacket input);

  /** Returns true if the strategy is complete. */
  public boolean isComplete() {
    return !tacticManager.hasTactic();
  }

  /** Informs the strategist that control has been taken away. It should pack up and go home. */
  void abort() {
    tacticManager.clearTactics();
  }

  /** Allow the strategist to execute the strategy. */
  public ControlsOutput execute(DataPacket input) {
    ControlsOutput output = new ControlsOutput();

    if (!tacticManager.hasTactic()) {
      bot.botRenderer.addAlertText("%s has no tactic", this.getType());
    } else {
      tacticManager.execute(input, output);
    }

    return output;
  }

  /** Returns the strategy tacticType. */
  public abstract Strategy.Type getType();
}
