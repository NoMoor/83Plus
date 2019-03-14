package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

public interface Tactician {

  void execute(ControlsOutput output, DataPacket input, Tactic nextTactic);
}
