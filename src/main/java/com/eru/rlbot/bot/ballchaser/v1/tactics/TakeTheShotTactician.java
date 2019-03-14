package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

// TODO: Needs to know where the ball is and where the goal is
// Relies on other low level tactical units to do the movement but this tactician is responsible for planning a shot on
// goal.
public class TakeTheShotTactician implements Tactician {
  @Override
  public void execute(ControlsOutput output, DataPacket input, Tactic nextTactic) {

  }
}
