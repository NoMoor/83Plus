package com.eru.rlbot.bot.ballchaser.v1.strats;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import rlbot.Bot;

/** Strategy responsible for patience. */
public class SupportStrategist extends Strategist {

  public SupportStrategist(EruBot bot) {
    super(bot);
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
    return Strategy.Type.SUPPORT;
  }
}
