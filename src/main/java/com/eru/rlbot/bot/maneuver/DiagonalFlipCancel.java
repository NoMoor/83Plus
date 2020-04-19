package com.eru.rlbot.bot.maneuver;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.common.RelativeUtils;
import com.eru.rlbot.bot.renderer.BotRenderer;
import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.bot.utils.Monitor;
import com.eru.rlbot.common.Numbers;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.Controls;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableSortedMap;
import java.awt.Color;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Performs a diagonal flip cancel.
 */
public class DiagonalFlipCancel extends Maneuver {

  private static final Logger logger = LogManager.getLogger("DiagonalFlipCancel");

  // Pitch/Yaw 1.0
  // 1060uu -> .239
  public static final double MIN_DRIFT = .26;
  public static final ImmutableSortedMap<Double, Double> PITCH_ANGLE_OFFSET =
      ImmutableSortedMap.<Double, Double>naturalOrder()
          .put(.2, 1.0)
          .put(.22, .9)
          .put(.24, .8)
          .put(.26, .7)
          .put(.28, .6)
          .put(.3, .5)
          .put(.32, .4)
          .put(.34, .3)
          .put(.36, .2)
          .put(.38, .1)
          .build();

  private final Vector3 target;
  private CarData initialState;
  private boolean complete;
  private Monitor monitor;
  private double pitch;

  public DiagonalFlipCancel(Builder builder) {
    this.target = builder.target;
  }

  @Override
  public void execute(DataPacket input, Controls output, Tactic tactic) {
    if (initialState == null) {
      initialState = input.car;
    }

    if (checkComplete(input)) {
      logger.debug("Pitch {} Jump {}", pitch, 30);
      if (monitor != null)
        monitor.trackWhile(!complete, input.car);
      return;
    }

    boolean boost = input.car.velocity.magnitude() < Constants.BOOSTED_MAX_SPEED;

    BotRenderer botRenderer = BotRenderer.forCar(input.car);
    JumpManager jumpManager = JumpManager.forCar(input.car);

    botRenderer.renderTarget(Color.RED, target);

    double correctionAngle = Angles.flatCorrectionAngle(input.car, target);
    correctionAngle += MIN_DRIFT * -Math.signum(correctionAngle);
    Vector3 noseRelative =
        RelativeUtils.translateRelative(input.car.position, target, input.car.orientation.getNoseVector());

    if (input.car.hasWheelContact && Math.abs(correctionAngle) > .1) {
      output
          .withBoost(boost)
          .withSteer(Math.signum(correctionAngle) * .6);
    } else if (input.car.hasWheelContact || (input.car.position.z < 25 && input.car.velocity.z > 0)) {
      botRenderer.setBranchInfo("Jump");
      output
          .withJump()
          .withBoost(boost)
          .withPitch(1);

      if (Math.abs(correctionAngle) > .05) {
        output.withYaw(Math.signum(correctionAngle) * .4);
      }
    } else if (!jumpManager.hasReleasedJumpInAir() || input.car.position.z < 54) {
      botRenderer.setBranchInfo("Release button");
      output
          .withBoost(boost)
          .withPitch(1);
      if (Math.abs(correctionAngle) > .01) {
        output.withYaw(Math.signum(correctionAngle) * .4);
      }
    } else if (jumpManager.canFlip()) {
      logger.debug("Deviation: {}", correctionAngle);
      monitor = Monitor.create(input);
      botRenderer.setBranchInfo("Flip");

      Map.Entry<Double, Double> ceilingEntry = PITCH_ANGLE_OFFSET.ceilingEntry(correctionAngle);
      Map.Entry<Double, Double> floorEntry = PITCH_ANGLE_OFFSET.floorEntry(correctionAngle);
      if (ceilingEntry != null && floorEntry != null) {
        double percent = (correctionAngle - floorEntry.getKey()) / (ceilingEntry.getKey() - floorEntry.getKey());
        pitch = -Numbers.lerp(floorEntry.getValue(), ceilingEntry.getValue(), percent);
      } else if (ceilingEntry != null) {
        pitch = -ceilingEntry.getValue();
      } else if (floorEntry != null) {
        pitch = -floorEntry.getValue();
      }

      output
          .withJump()
          .withBoost(boost)
          .withPitch(pitch) // Front flip
          .withYaw(Math.signum(noseRelative.x) * -1) // TODO: adjust this to be relative to the target.
          .withThrottle(1.0);
    } else if (jumpManager.isFlipping()) {
      botRenderer.setBranchInfo("Flip Cancel");
      output
          .withBoost(boost)
          .withPitch(1); // Flip cancel.
    } else {
      botRenderer.setBranchInfo("Land Cleanly");
      output
          .withBoost(boost)
          .withSlide();
      Angles3.setControlsFor(input.car, Orientation.fromFlatVelocity(target.minus(input.car.position)), output);
    }
  }

  private boolean checkComplete(DataPacket input) {
    complete = (input.car != initialState
        && input.car.hasWheelContact
        && input.car.position.distance(initialState.position) > 700);
    return complete;
  }

  @Override
  public boolean isComplete() {
    return complete;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Vector3 target;

    public Builder setTarget(Vector3 target) {
      this.target = target;
      return this;
    }

    public DiagonalFlipCancel build() {
      return new DiagonalFlipCancel(this);
    }
  }
}
