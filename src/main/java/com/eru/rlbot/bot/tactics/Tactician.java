package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.common.PathExecutor;
import com.eru.rlbot.bot.main.Agc;
import com.eru.rlbot.bot.maneuver.Maneuver;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

public abstract class Tactician {

  protected final Agc bot;
  protected final TacticManager tacticManager;
  protected final PathExecutor pathExecutor;

  protected Tactic lastTactic;
  protected Maneuver delegate;

  Tactician(Agc bot, TacticManager tacticManager) {
    this.bot = bot;
    this.tacticManager = tacticManager;
    this.pathExecutor = PathExecutor.forTactician(this);
  }

  void execute(DataPacket input, ControlsOutput output, Tactic tactic) {
    checkTactic(input, tactic);
    if (!useDelegate(input, output, tactic)) {
      internalExecute(input, output, tactic);

      // Try delegate if it was assigned in the internal execution.
      useDelegate(input, output, tactic);
    }
  }

  private boolean useDelegate(DataPacket input, ControlsOutput output, Tactic tactic) {
    if (delegate != null) {
      delegate.execute(input, output, tactic);
      if (delegate.isComplete()) {
        delegate = null;
      }
      return true;
    }
    return false;
  }

  abstract void internalExecute(DataPacket input, ControlsOutput output, Tactic nextTactic);

  protected void delegateTo(Maneuver delegate) {
    this.delegate = delegate;
  }

  public boolean isLocked() {
    return false;
  }

  public TacticManager getTacticManager() {
    return tacticManager;
  }

  protected void checkTactic(DataPacket input, Tactic tactic) {
    if (lastTactic == null || !lastTactic.equals(tactic)) {
      reset(input);
      lastTactic = tactic;
    }
  }

  protected void clearDelegate() {
    delegate = null;
  }

  protected void reset(DataPacket input) {
    clearDelegate();
  }
}
