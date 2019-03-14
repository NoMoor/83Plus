package com.eru.rlbot.bot.ballchaser.v1.strats;

import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import rlbot.Bot;

/** Responsible for shadowing, blocking, shots, and clearing. */
public class DefendStategist implements Strategist {
  public DefendStategist(Bot bot) {

  }

  @Override
  public boolean assign(DataPacket input) {
    return false;
  }

  @Override
  public boolean isComplete(DataPacket input) {
    return false;
  }

  @Override
  public void abort() {

  }

  @Override
  public ControlsOutput execute(DataPacket input) {
    return null;
  }

  @Override
  public Strategy.Type getType() {
    return Strategy.Type.DEFEND;
  }
}
