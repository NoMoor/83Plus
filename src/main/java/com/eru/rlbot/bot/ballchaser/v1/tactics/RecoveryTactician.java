package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

public class RecoveryTactician implements Tactician {

  @Override
  public void execute(DataPacket input, ControlsOutput output, Tactic nextTactic) {
    // TODO(ahatfield): Add a threshold for how far you are from the ground.
    // The car is falling.
    if (!input.car.hasWheelContact && input.car.velocity.z < 0) {

      double yawCorrection =
          input.car.velocity.flatten().correctionAngle(input.car.orientation.getNoseVector().flatten());

      // If nose vector is down, pitch up
      output
          .withPitch(-1 * input.car.orientation.getNoseVector().z) // TODO(ahatfield): Adjust this based on correction needed.
          .withRoll(input.car.orientation.getRightVector().z) // If the door is pointing down, roll left.
          .withYaw((float) yawCorrection);
    }
  }
}
