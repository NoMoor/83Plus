package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.main.EruBot;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;

public class JumpTactician extends Tactician {

  private Vector3 targetPosition;

  JumpTactician(EruBot bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  void setTarget(Vector3 targetPosition) {
    // TODO: Ensure this is called.
    this.targetPosition = targetPosition;
  }

  @Override
  public void execute(DataPacket input, ControlsOutput output, Tactic nextTactic) {
    if (input.car.position.distance(targetPosition) < 1000 && input.car.position.z > 1000) {
      output
          .withJump()
          .withBoost();
    } else if (input.car.boost > 50 && !input.car.hasWheelContact) {
      output.withBoost();
    }
  }
}
