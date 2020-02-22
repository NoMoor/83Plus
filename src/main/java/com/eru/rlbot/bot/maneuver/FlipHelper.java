package com.eru.rlbot.bot.maneuver;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.main.Agc;
import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.ControlsOutput;

public class FlipHelper extends Maneuver {

  private static final double EIGHTH = Math.PI / 4;
  private static final double QUARTER = Math.PI / 2;
  private final double aggressiveness;

  private Agc bot;
  private boolean flipComplete;
  private boolean done;

  public FlipHelper(Builder builder) {
    this.bot = builder.bot;
    this.aggressiveness = builder.aggressiveness;
  }

  public static Builder builder(Agc bot) {
    return new Builder(bot);
  }

  @Override
  public void execute(DataPacket input, ControlsOutput output, Tactic tactic) {
    if (done) {
      // Do nothing.
      return;
    }

    JumpManager jumpManager = JumpManager.forCar(input.car);

    if (flipComplete) {
      if (input.car.hasWheelContact) {
        bot.botRenderer.setBranchInfo("Flip complete");
        done = true;
      } else {
        bot.botRenderer.setBranchInfo("Waiting to land");
        float noseZ = input.car.orientation.getNoseVector().z;
        output
            .withBoost(-.1 < noseZ && noseZ < .2)
            .withThrottle(1.0f);
        Angles3.setControlsFor(input.car, Orientation.fromFlatVelocity(input.car).getOrientationMatrix(), output);
      }
    } else {
      if (!input.car.jumped) {
        bot.botRenderer.setBranchInfo("Initial Jump");
        // Jump now
        output
            .withJump(!jumpManager.jumpPressedLastFrame())
            .withThrottle(1.0)
            .withBoost();
      } else if (jumpManager.getJumpCount() * (1 - aggressiveness) < JumpManager.MAX_HEIGHT_TICKS && input.car.velocity.z >= 0 && !jumpManager.hasMaxJumpHeight()) {
        bot.botRenderer.setBranchInfo("Hold Jump");
        output
            .withJump()
            .withBoost();
      } else if (!jumpManager.hasReleasedJumpInAir()) {
        bot.botRenderer.setBranchInfo("Quick release");
        output.withBoost();
        // Release Jump
      } else if (jumpManager.canFlip()) {
        bot.botRenderer.setBranchInfo("Do flip");
        // TODO: This doesn't work for half flips...
        double velocityCorrectionAngle =
            Angles.flatCorrectionAngle(input.car.position, input.car.velocity, tactic.subject.position);
        // TODO: Adjust this correction based on how fast we are going and angle flipping.
        velocityCorrectionAngle *= 2;

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

        output
            .withSteer(flipYaw)
            .withYaw(flipYaw)
            .withJump()
            .withPitch(flipPitch);
        flipComplete = true;
      }
    }
  }

  @Override
  public boolean isComplete() {
    return done;
  }

  public static class Builder {
    private final Agc bot;
    private double aggressiveness;

    Builder(Agc bot) {
      this.bot = bot;
    }

    public Builder setAggressiveness(double aggressiveness) {
      this.aggressiveness = aggressiveness;
      return this;
    }

    public FlipHelper build() {
      return new FlipHelper(this);
    }
  }
}
