package com.eru.rlbot.bot.maneuver;

import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.common.BotRenderer;
import com.eru.rlbot.bot.common.Monitor;
import com.eru.rlbot.bot.common.NormalUtils;
import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableSortedMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DiagonalFlipCancel extends Maneuver {

  private static final Logger logger = LogManager.getLogger("DiagonalFlipCancel");

  // Jump height 30 and ground speed 1080
  public static final double MIN_DRIFT = 480d;
  public static final ImmutableSortedMap<Double, Double> PITCH_X_OFFSET =
      ImmutableSortedMap.<Double, Double>naturalOrder()
          .put(MIN_DRIFT, 1.0)
          .put(508d, .9)
          .put(540d, .8)
          .put(561d, .7)
          .put(590d, .6)
          .put(610d, .5)
          .put(646d, .4)
          .put(670d, .3)
          .put(701d, .2)
          .put(714d, .1)
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
  public void execute(DataPacket input, ControlsOutput output, Tactic tactic) {
    if (initialState == null) {
      initialState = input.car;
      monitor = Monitor.create(input);
    }

    if (checkComplete(input)) {
      logger.info("Pitch {} Jump {}", pitch, 30);
      monitor.trackWhile(!complete, input.car);
      return;
    }

    BotRenderer botRenderer = BotRenderer.forCar(input.car);
    JumpManager jumpManager = JumpManager.forCar(input.car);

    Vector3 noseRelative =
        NormalUtils.translateRelative(input.car.position, target, input.car.orientation.getNoseVector());
    if (input.car.hasWheelContact && Math.abs(noseRelative.x) < MIN_DRIFT) {
      output
          .withBoost()
          .withSteer(Math.signum(noseRelative.x) * .5);
    } else if (input.car.hasWheelContact || (input.car.position.z < 40 && input.car.velocity.z > 0)) {
      botRenderer.setBranchInfo("Jump");
      output
          .withJump()
          .withBoost()
          .withPitch(1);
    } else if (!jumpManager.hasReleasedJumpInAir()) {
      botRenderer.setBranchInfo("Release button");
      output
          .withBoost()
          .withPitch(1)
          .withYaw(Math.signum(noseRelative.x) * -1);
    } else if (jumpManager.canFlip()) {
      botRenderer.setBranchInfo("Flip");

      double relativeX = Math.abs(noseRelative.x);
      Map.Entry<Double, Double> ceilingEntry = PITCH_X_OFFSET.ceilingEntry(relativeX);
      Map.Entry<Double, Double> floorEntry = PITCH_X_OFFSET.floorEntry(relativeX);
      if (ceilingEntry != null && floorEntry != null) {
        double percent = (relativeX - floorEntry.getKey()) / (ceilingEntry.getKey() - floorEntry.getKey());
        pitch = -Angles3.lerp(floorEntry.getValue(), ceilingEntry.getValue(), percent);
      } else if (ceilingEntry != null) {
        pitch = -ceilingEntry.getValue();
      } else if (floorEntry != null) {
        pitch = -floorEntry.getValue();
      }

      output
          .withJump()
          .withBoost()
          .withPitch(pitch) // Front flip
          .withYaw(Math.signum(noseRelative.x) * -1) // TODO: adjust this to be relative to the target.
          .withThrottle(1.0);
    } else if (jumpManager.isFlipping()) {
      botRenderer.setBranchInfo("Flip Cancel");
      output
          .withBoost()
          .withPitch(1); // Flip cancel.
    } else {
      botRenderer.setBranchInfo("Land Cleanly");
      output
          .withBoost()
          .withSlide();
      Angles3.setControlsForFlatLanding(input.car, output);
//      Angles3.setControlsFor(input.car, Orientation.fromFlatVelocity(target.minus(input.car.position)), output);
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
