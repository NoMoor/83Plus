package com.eru.rlbot.bot.maneuver;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.renderer.BotRenderer;
import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.Controls;
import com.eru.rlbot.common.vector.Vector3;

/**
 * Executes a half-flip toward the given target.
 */
public class HalfFlip extends Maneuver {

  private final boolean boost;
  private final Vector3 target;

  private boolean done;
  private boolean roll;
  private float rollDirection = -1;

  public HalfFlip(Builder builder) {
    this.boost = builder.boost;
    this.target = builder.target;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public void execute(DataPacket input, Controls output, Tactic tactic) {
    JumpManager jumpManager = JumpManager.forCar(input.car);
    BotRenderer botRenderer = BotRenderer.forCar(input.car);

    Vector3 localAngular = input.car.orientation.localCoordinates(input.car.angularVelocity);
    float velocityCorrection = (float) Angles.flatCorrectionAngle(input.car.position, input.car.velocity, target);
    float noseCorrection = (float) Angles.flatCorrectionAngle(input.car, target);
    boolean isPointingTowardTarget = Math.abs(noseCorrection) < .15;
    boolean needsBoost = boost && !input.car.isSupersonic;

    if (!roll) {
      if (input.car.velocity.y < 1000 && input.car.hasWheelContact) {
        output.withThrottle(-1);
      } else if (!input.car.jumped) {
        botRenderer.setBranchInfo("Jump");
        output
            .withJump()
            .withThrottle(-1);
      } else if (!jumpManager.hasReleasedJumpInAir()) {
        output
            .withThrottle(-1)
            .withJump(input.car.position.z < 50);
        botRenderer.setBranchInfo("Release Jump");
        // do nothing
      } else if (jumpManager.canFlip()) {
        botRenderer.setBranchInfo("Flip");
        // TODO: Adjust flip angle based on target.
        output
            .withThrottle(-1)
            .withJump()
            .withPitch(1);
      } else if (input.car.orientation.getRoofVector().z > .3 && jumpManager.isFlipping()) {
        output.withThrottle(1);
        // Do nothing.
      } else if (Math.abs(input.car.orientation.getNoseVector().z) > .5 || Math.abs(localAngular.y) > 1.5) {
        botRenderer.setBranchInfo("Flip cancel");
        output
            .withThrottle(1)
            .withPitch(-Math.signum(input.car.orientation.getNoseVector().z))
            .withBoost(Math.abs(input.car.orientation.getNoseVector().z) < .6 && needsBoost && isPointingTowardTarget);
      } else {
        roll = true;
        if (Math.abs(velocityCorrection) > .1) {
          rollDirection = -Math.signum(velocityCorrection);
        }
      }
    } else if (!input.car.hasWheelContact) {
      float roofZ = input.car.orientation.getRoofVector().z;
      botRenderer.setBranchInfo("Rotate to land");

      if (roofZ < -.2) {
        output
            .withThrottle(1)
            .withRoll(rollDirection)
            .withPitch(Math.signum(localAngular.y))
            .withBoost(needsBoost && isPointingTowardTarget);
      } else {
//        Angles3.setControlsFor(input.car, Orientation.fromFlatVelocity(target.minus(input.car.position)), output);
        Angles3.setControlsFor(input.car, Orientation.fromFlatVelocity(input.car.velocity), output);
        output
            .withBoost(needsBoost && isPointingTowardTarget)
            .withThrottle(1);
      }
    } else {
      botRenderer.setBranchInfo("Done");
      output
          .withThrottle(1.0)
          .withBoost(needsBoost);
      done = true;
    }
  }

  @Override
  public boolean isComplete() {
    return done;
  }

  /**
   * A builder for the half-flip maneuver.
   */
  public static class Builder {

    private boolean boost;
    private Vector3 target;

    private Builder() {
    }

    /**
     * Sets the target to flip toward.
     */
    public Builder withTarget(Vector3 target) {
      this.target = target;
      return this;
    }

    /**
     * Sets whether or not to use boost.
     */
    public Builder withBoost(boolean value) {
      this.boost = value;
      return this;
    }

    /**
     * Sets use boost to true.
     */
    public Builder withBoost() {
      return withBoost(true);
    }

    /**
     * Returns a half-flip maneuver.
     */
    public HalfFlip build() {
      return new HalfFlip(this);
    }
  }
}
