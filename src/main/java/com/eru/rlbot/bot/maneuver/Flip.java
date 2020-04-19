package com.eru.rlbot.bot.maneuver;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.renderer.BotRenderer;
import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.Controls;
import com.eru.rlbot.common.vector.Vector3;

/**
 * Helper for doing flip maneuvers.
 */
public class Flip extends Maneuver {

  private static final double EIGHTH = Math.PI / 4;
  private static final double QUARTER = Math.PI / 2;
  private final double aggressiveness;
  private final Vector3 target;

  // Null if they should be determined by the helper based on the target position.
  private final Double yaw;
  private final Double pitch;

  private int initialJump;
  private boolean flipComplete;
  private boolean done;
  private boolean flipAtTarget;

  public Flip(Builder builder) {
    this.aggressiveness = builder.aggressiveness;
    this.target = builder.target;
    this.pitch = builder.pitch;
    this.yaw = builder.yaw;
    this.flipAtTarget = builder.flipAtTarget;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public void execute(DataPacket input, Controls output, Tactic tactic) {
    BotRenderer botRenderer = BotRenderer.forCar(input.car);
    JumpManager jumpManager = JumpManager.forCar(input.car);

    output.withThrottle(1.0);

    if (done) {
      // Do nothing.
      return;
    }

    double framesToTarget = (input.car.position.distance(target) / input.car.groundSpeed) * Constants.STEP_SIZE_COUNT;
    boolean flipIntoTarget = flipAtTarget && framesToTarget < 13;
    boolean maxHeightJump = jumpManager.hasMaxJumpHeight();
    boolean flipImmediately = !flipAtTarget && input.car.position.z > 50;

    boolean holdJump = !(flipIntoTarget || maxHeightJump || flipImmediately) && !jumpManager.canFlip();

    if (flipComplete) {
      if (input.car.hasWheelContact) {
        done = true;
      } else {
        botRenderer.setBranchInfo("Waiting to land");
        float noseZ = input.car.orientation.getNoseVector().z;
        boolean facingForward = input.car.orientation.getNoseVector().dot(input.car.velocity) > 0;
        output
            .withBoost(-.1 < noseZ && noseZ < .2 && facingForward && !input.car.isSupersonic && input.car.boost > 50)
            .withThrottle(1.0f);

        if (!jumpManager.isFlipping()) {
          // TODO: Replace this with generic landing helper.
          Angles3.setControlsFor(input.car, Orientation.fromFlatVelocity(input.car), output);
        }
      }
    } else {
      if (!input.car.jumped) {
        initialJump++;
        botRenderer.setBranchInfo("Initial Jump");

        // Jump now
        output
            .withJump(true);

        if (initialJump > 5) {
          output.withJump(false);
          done = true;
          return;
        }
      } else if (holdJump) {
        botRenderer.setBranchInfo("Hold Jump");
        output
            .withThrottle(1.0)
            .withJump();
      } else if (!jumpManager.hasReleasedJumpInAir()) {
        botRenderer.setBranchInfo("Quick release");
        output.withThrottle(1.0);
        // Release Jump
      } else if (jumpManager.canFlip()) {
        if (this.flipAtTarget && !flipIntoTarget) {
          return;
        }

        botRenderer.setBranchInfo("Do flip");

        if (pitch != null && yaw != null) {
          output
              .withJump()
              .withPitch(pitch)
              .withYaw(yaw);
        } else {
          double velocityCorrectionAngle =
              Angles.flatCorrectionAngle(input.car.position, input.car.velocity, getTarget(tactic));
          // TODO: Adjust this correction based on how fast we are going and angle flipping.
          // velocityCorrectionAngle *= 2;

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
            flipPitch = -Math.abs(QUARTER - Math.abs(totalCorrection)) / EIGHTH;
          }

          output
              .withYaw(flipYaw)
              .withJump()
              .withPitch(flipPitch);
        }

        flipComplete = true;
      }
    }
  }

  private Vector3 getTarget(Tactic tactic) {
    return target != null ? target : tactic.subject.position;
  }

  @Override
  public boolean isComplete() {
    return done;
  }

  public static class Builder {
    private boolean flipAtTarget = true;
    private double aggressiveness;
    private Vector3 target;
    private Double yaw;
    private Double pitch;

    private Builder() {
    }

    public Builder setAggressiveness(double aggressiveness) {
      this.aggressiveness = aggressiveness;
      return this;
    }

    public Builder setTarget(Vector3 target) {
      this.target = target;
      return this;
    }

    public Builder withFixedYaw(double yaw) {
      this.yaw = yaw;
      return this;
    }

    public Builder withFixedPitch(double pitch) {
      this.pitch = pitch;
      return this;
    }

    public Builder flipEarly() {
      this.flipAtTarget = false;
      return this;
    }

    public Flip build() {
      return new Flip(this);
    }
  }
}
