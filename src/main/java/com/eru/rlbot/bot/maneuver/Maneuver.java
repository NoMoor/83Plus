package com.eru.rlbot.bot.maneuver;

import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

public abstract class Maneuver {
  public abstract void execute(DataPacket input, ControlsOutput output, Tactic tactic);

  public abstract boolean isComplete();
}
