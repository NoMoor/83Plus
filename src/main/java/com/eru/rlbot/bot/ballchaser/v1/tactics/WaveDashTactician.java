package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

public class WaveDashTactician implements Tactician {

  enum Stage {
    PRE_JUMP,
    JUMP,
    TILT,
    DODGE,
    SLIDE
  }

  private final EruBot bot;

  private Stage currentStage = Stage.PRE_JUMP;

  WaveDashTactician(EruBot bot) {
    this.bot = bot;
  }

  @Override
  public void execute(ControlsOutput output, DataPacket input, Tactic nextTactic) {
    
    boolean stageComplete = doCurrentStage();

    bot.botRenderer.setBranchInfo(currentStage.name());

    if (stageComplete) {
      currentStage = Stage.values()[(currentStage.ordinal() + 1) % Stage.values().length];
    }
  }

  private boolean doCurrentStage() {
    switch (currentStage) {
      case PRE_JUMP:
        return preJump();
      case JUMP:
        return jump();
      case TILT:
        return tilt();
      case DODGE:
        return dodge();
      case SLIDE:
        return slide();
    }
    throw new IllegalStateException("Not sure which stage we are in.");
  }

  private boolean slide() {
    return false;
  }

  private boolean dodge() {
    return false;
  }

  private boolean tilt() {
    return false;
  }

  private boolean jump() {
    return false;
  }

  private boolean preJump() {
    return false;
  }
}
