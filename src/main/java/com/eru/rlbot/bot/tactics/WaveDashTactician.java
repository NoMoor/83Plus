package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.common.Matrix3;
import com.eru.rlbot.bot.main.Agc;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.output.ControlsOutput;

public class WaveDashTactician extends Tactician {

  private Tactic currentTactic;

  enum Stage {
    PRE_JUMP,
    JUMP,
    TILT_DODGE,
    SLIDE
  }

  private Stage currentStage = Stage.PRE_JUMP;

  WaveDashTactician(Agc bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  @Override
  public void execute(DataPacket input, ControlsOutput output, Tactic nextTactic) {
    if (!nextTactic.equals(currentTactic)) {
      currentTactic = nextTactic;
      if (input.car.hasWheelContact) {
        currentStage = Stage.PRE_JUMP;
      } else {
        currentStage = Stage.TILT_DODGE;
      }
    }

    bot.botRenderer.setBranchInfo(currentStage.name());

    boolean stageComplete = doCurrentStage(input, output);

    if (stageComplete) {
      int nextOrdinal = currentStage.ordinal() + 1;

      currentStage = Stage.values()[nextOrdinal % Stage.values().length];
    }
  }

  private boolean doCurrentStage(DataPacket input, ControlsOutput output) {
    switch (currentStage) {
      case PRE_JUMP:
        return preJump(input, output);
      case JUMP:
        return jump(input, output);
      case TILT_DODGE:
        return tiltDodge(input, output);
      case SLIDE:
        return slide(input, output);
    }
    throw new IllegalStateException("Not sure which stage we are in.");
  }

  private int slideTicks = 0;
  private boolean slide(DataPacket input, ControlsOutput output) {
    output.withSlide();

    if (input.car.hasWheelContact && slideTicks > SLIDE_TICKS) {
      slideTicks = 0;
      if (input.car.groundSpeed > 1000) {
        output.withJump();
        currentStage = Stage.JUMP;
      }
      return true;
    }

    slideTicks++;
    return false;
  }

  private static final int SLIDE_TICKS = 10;

  private static final float IDEAL_PITCH = -.05f;
  private static final float IDEAL_ROLL = .55f;
  private static final float IDEAL_YAW_OFFSET = .3f;

  private static final float DODGE_PITCH = .1f;
  private static final float DODGE_YAW = -1f;

  private static final float DODGE_HEIGHT = 5;

  private int tiltTicks = 0;

  private boolean tiltDodge(DataPacket input, ControlsOutput output) {
    if (tiltTicks > 10 && input.car.hasWheelContact) { // Wait a few ticks to make sure we catch the jump.
      // This should not happen.
      tiltTicks = 0;
      currentStage = Stage.SLIDE;
      return true;
    }

    float groundTicks = -(input.car.position.z + DODGE_HEIGHT) / input.car.velocity.z;

    if (input.car.velocity.z < 0 && Math.abs(groundTicks) < .15) {
      // Dodge
      output.withJump()
          .withYaw(DODGE_YAW)
          .withPitch(DODGE_PITCH)
          .withSlide();
      return true;
    }

    Matrix3 target =
        Orientation.convert(
            IDEAL_PITCH,
            (3.14 / 2) + IDEAL_YAW_OFFSET /* + travelToTargetOffset(input) */,
            IDEAL_ROLL).getOrientationMatrix();

    Angles3.setControlsFor(input.car, target, output);

    tiltTicks++;
    return false;
  }

  private int hasJumpedTicks;
  private boolean jump(DataPacket input, ControlsOutput output) {
    if (hasJumpedTicks == 0) {
      output.withJump()
          .withSlide();
    } else {
      // Wait until we are in the air for sure.
      if (!input.car.hasWheelContact) {
        hasJumpedTicks = 0;
        return true;
      }
      output.withSlide();
    }

    hasJumpedTicks++;
    if (hasJumpedTicks > 2) { // Incase we somehow get stuck here. Don't wait forever.
      hasJumpedTicks = 0;
    }

    return false;
  }

  private double travelToTargetOffset(DataPacket input) {
    return input.car.orientation.getNoseVector().flatten().correctionAngle(currentTactic.getTargetPosition().flatten());
  }

  private double travelOffset(DataPacket input) {
    return input.car.orientation.getNoseVector().flatten().correctionAngle(input.car.velocity.normalized().flatten());
  }

  private int preJumpTicks = 0;
  private boolean preJump(DataPacket input, ControlsOutput output) {
    if (!input.car.hasWheelContact) {
      preJumpTicks = 0;
      // We are already in the air.
      return true;
    }
    output.withThrottle(1.0f);

    preJumpTicks++;
    if (preJumpTicks > 0 && input.car.groundSpeed > 1200) {
      preJumpTicks = 0;
      return true;
    }
    return false;
  }
}
