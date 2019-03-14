package com.eru.rlbot.bot.ballchaser.v1.strats;

import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

public interface Strategist {
  boolean assign(DataPacket input);

  boolean isComplete(DataPacket input);

  ControlsOutput execute(DataPacket input);

  Strategy.Type getType();
}
