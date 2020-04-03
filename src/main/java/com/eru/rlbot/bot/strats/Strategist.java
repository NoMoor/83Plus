package com.eru.rlbot.bot.strats;

import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
import com.eru.rlbot.bot.tactics.TacticManager;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.Controls;

public abstract class Strategist {

  protected final ApolloGuidanceComputer bot;
  protected final TacticManager tacticManager;

  protected Strategist(ApolloGuidanceComputer bot) {
    this.bot = bot;
    this.tacticManager = new TacticManager(bot);
  }

  /** Assigns control to the given strategist. If the strategist is unable to accept control, it can return false. */
  abstract boolean assign(DataPacket input);

  /**
   * Returns true if the strategy is complete.
   */
  public boolean isComplete(DataPacket input) {
    return !tacticManager.hasTactic();
  }

  /** Informs the strategist that control has been taken away. It should pack up and go home. */
  void abort() {
    tacticManager.clearTactics();
  }

  /**
   * Allow the strategist to execute the strategy.
   */
  public Controls execute(DataPacket input) {
    Controls output = Controls.create();

    if (!tacticManager.hasTactic()) {
      bot.botRenderer.addAlertText("%s has no tactic", this.getType());
    } else {
      tacticManager.execute(input, output);
    }

    return output;
  }

  /**
   * Returns the strategy tacticType.
   */
  public abstract Strategy.Type getType();

  /**
   * Returns the next strategy to use.
   */
  public abstract Strategy.Type getDelegate();
}
