package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.main.Agc;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.ControlsOutput;

/** Handles flip tactics for front and diagonal flips. */
public class FlipTactician extends Tactician {

  private static final double EIGHTH = Math.PI / 4;
  private static final double QUARTER = Math.PI / 2;

  private boolean isLocked = true;
  private boolean flipComplete;

  public FlipTactician(Agc bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  @Override
  public boolean isLocked() {
    return isLocked;
  }

  @Override
  public void execute(DataPacket input, ControlsOutput output, Tactic tactic) {
    JumpManager jumpManager = JumpManager.forCar(input.car);

    if (flipComplete) {
      if (input.car.hasWheelContact) {
        bot.botRenderer.setBranchInfo("Flip complete");
        isLocked = false;
        tacticManager.setTacticComplete(tactic);
      } else {
        bot.botRenderer.setBranchInfo("Waiting to land");
        float noseZ = input.car.orientation.getNoseVector().z;
        output
            .withBoost(0 < noseZ && noseZ < .2)
            .withThrottle(1.0f);
        Angles3.setControlsFor(input.car, Orientation.fromFlatVelocity(input.car).getOrientationMatrix(), output);
      }
    } else {
      if (!input.car.jumped) {
        bot.botRenderer.setBranchInfo("Initial Jump");
        // Jump now
        output
            .withJump(!jumpManager.jumpPressedLastFrame())
            .withThrottle(1.0);
      } else if (input.car.position.z < 50 && input.car.velocity.z >= 0) {
        bot.botRenderer.setBranchInfo("Hold Jump");
        output
            .withJump()
            .withBoost();
      } else if (!jumpManager.canFlip() && !jumpManager.hasReleasedJumpInAir()) {
        bot.botRenderer.setBranchInfo("Quick release");
        // Release Jump
      } else if (jumpManager.canFlip()) {
        bot.botRenderer.setBranchInfo("Do flip");
        // TODO: This doesn't work for half flips...
        double velocityCorrectionAngle =
            Angles.flatCorrectionAngle(input.car.position, input.car.velocity, tactic.subject.position);
        // TODO: Adjust this correction based on how fast we are going and angle flipping.
        velocityCorrectionAngle *=2;

        double noseVelocityAngle = input.car.orientation.getNoseVector().flatten()
            .correctionAngle(input.car.velocity.flatten());
        double totalCorrection = velocityCorrectionAngle + noseVelocityAngle;

        double flipYaw, flipPitch;
        if (Math.abs(totalCorrection) < EIGHTH) {
          // Less than 45 degree flip.
          flipPitch = -1;
          flipYaw = totalCorrection / EIGHTH;
        } else {
          flipYaw = Math.signum(totalCorrection);
          flipPitch = -(QUARTER - Math.abs(totalCorrection)) / EIGHTH;
        }

//        bot.botRenderer.addAlertText("Flipped at %f %f %f", totalCorrection, flipYaw, flipPitch);

        output
            .withSteer(flipYaw)
            .withYaw(flipYaw)
            .withJump()
            .withPitch(flipPitch);
        flipComplete = true;
      }
    }
  }
}


