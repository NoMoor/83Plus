package com.eru.rlbot.bot.ballchaser.v1.strats;

import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import rlbot.Bot;

/** Strategy responsible for patience. */
public class SupportStategist implements Strategist {

  public SupportStategist(Bot bot) {

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
  public ControlsOutput execute(DataPacket input) {
    return null;
  }

  @Override
  public Strategy.Type getType() {
    return Strategy.Type.SUPPORT;
  }
}
