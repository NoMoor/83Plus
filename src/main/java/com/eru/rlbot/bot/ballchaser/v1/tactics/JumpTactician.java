package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;

public class JumpTactician implements Tactician {

  private Vector3 targetPosition;

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
