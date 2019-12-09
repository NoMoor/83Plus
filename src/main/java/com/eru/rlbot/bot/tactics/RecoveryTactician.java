package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.main.Acg;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

public class RecoveryTactician extends Tactician {

  RecoveryTactician(Acg bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  @Override
  public void execute(DataPacket input, ControlsOutput output, Tactic nextTactic) {
    // TODO(ahatfield): Update to use Angles3.
    // The car is falling.
    if (!input.car.hasWheelContact && input.car.velocity.z < 0) {

      double yawCorrection =
          input.car.velocity.flatten().correctionAngle(input.car.orientation.getNoseVector().flatten());

      // If nose vector is down, pitch up
      output
          .withPitch(-1 * input.car.orientation.getNoseVector().z) // TODO(ahatfield): Adjust this based on correction needed.
          .withRoll(input.car.orientation.getLeftVector().z) // If the door is pointing down, roll left.
          .withYaw((float) yawCorrection);
    }
  }
}
