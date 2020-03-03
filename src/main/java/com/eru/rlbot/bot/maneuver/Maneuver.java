package com.eru.rlbot.bot.maneuver;

import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.Controls;

/**
 * Abstract class for performing a given maneuver.
 */
public abstract class Maneuver {

  /**
   * Performs the action.
   */
  public abstract void execute(DataPacket input, Controls output, Tactic tactic);

  /**
   * Return true if the maneuver is complete. This maneuver will have control until it indicates complete.
   */
  public abstract boolean isComplete();
}
