package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
import com.eru.rlbot.bot.maneuver.Maneuver;
import com.eru.rlbot.bot.path.PathExecutor;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.Controls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Tactician {

  private static final Logger logger = LogManager.getLogger("Tactician");

  protected final ApolloGuidanceComputer bot;
  protected final TacticManager tacticManager;
  protected final PathExecutor pathExecutor;

  protected Tactic lastTactic;
  protected Maneuver delegate;

  Tactician(ApolloGuidanceComputer bot, TacticManager tacticManager) {
    this.bot = bot;
    this.tacticManager = tacticManager;
    this.pathExecutor = PathExecutor.forTactician(this);
  }

  void execute(DataPacket input, Controls output, Tactic tactic) {
    checkTactic(input, tactic);
    if (!useDelegate(input, output, tactic)) {
      internalExecute(input, output, tactic);

      // Try delegate if it was assigned in the internal execution.
      useDelegate(input, output, tactic);
    }
  }

  private boolean useDelegate(DataPacket input, Controls output, Tactic tactic) {
    if (delegate != null) {
      delegate.execute(input, output, tactic);
      if (delegate.isComplete()) {
        delegate = null;
      }
      return true;
    }
    return false;
  }

  abstract void internalExecute(DataPacket input, Controls output, Tactic nextTactic);

  protected void delegateTo(Maneuver delegate) {
    this.delegate = delegate;
  }

  public void requestDelegate(Maneuver delegate) {
    if (allowDelegate()) {
      delegateTo(delegate);
    } else {
      logger.warn("Delegate {} rejected for tactician {}", delegate.getClass(), this.getClass());
    }
  }

  public boolean allowDelegate() {
    return false;
  }

  public boolean isLocked() {
    return delegate != null && !delegate.isComplete();
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
