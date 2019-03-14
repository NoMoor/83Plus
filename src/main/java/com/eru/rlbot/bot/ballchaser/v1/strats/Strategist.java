package com.eru.rlbot.bot.ballchaser.v1.strats;

import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

public interface Strategist {

  /** Assigns control to the given strategist. If the strategist is unable to accept control, it can return false. */
  boolean assign(DataPacket input);

  /** Returns true if the strategy is complete. */
  boolean isComplete(DataPacket input);

  /** Informs the strategist that control has been taken away. It should pack up and go home. */
  void abort();

  /** Allow the strategist to execute the strategy. */
  ControlsOutput execute(DataPacket input);

  /** Returns the strategy type. */
  Strategy.Type getType();
}
