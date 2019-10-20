package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;

public class WaveDashTactician implements Tactician {

  private Tactic currentTactic;

  enum Stage {
    PRE_JUMP,
    JUMP,
    TILT,
    DODGE,
    SLIDE
  }

  private final EruBot bot;

  private Stage currentStage = Stage.PRE_JUMP;

  WaveDashTactician(EruBot bot) {
    this.bot = bot;
  }

  @Override
  public void execute(DataPacket input, ControlsOutput output, Tactic nextTactic) {
    if (nextTactic != currentTactic) {
      bot.botRenderer.addAlertText("New Wavedash Tactic");
      currentTactic = nextTactic;
      if (input.car.hasWheelContact) {
        currentStage = Stage.PRE_JUMP;
      } else {
        currentStage = Stage.TILT;
      }
    }

    boolean stageComplete = doCurrentStage(input, output);
    // Hold slide if we are traveling at all sideways.
    output.withSlide(input.car.groundSpeed > 800 && travelOffset(input) > .05);

    bot.botRenderer.setBranchInfo(currentStage.name());

    if (stageComplete) {
      currentStage = Stage.values()[(currentStage.ordinal() + 1) % Stage.values().length];
    }
  }

  private boolean doCurrentStage(DataPacket input, ControlsOutput output) {
    switch (currentStage) {
      case PRE_JUMP:
        return preJump(input, output);
      case JUMP:
        return jump(input, output);
      case TILT:
        return tilt(input, output);
      case DODGE:
        return dodge(input, output);
      case SLIDE:
        return slide(input, output);
    }
    throw new IllegalStateException("Not sure which stage we are in.");
  }

  private int slideTicks = 0;
  private boolean slide(DataPacket input, ControlsOutput output) {
    output.withSlide();

    if (input.car.hasWheelContact && slideTicks > 0) {
      slideTicks = 0;
      return true;
    }

    slideTicks++;
    return false;
  }

  private boolean dodge(DataPacket input, ControlsOutput output) {
    if (input.car.hasWheelContact) {
      // This should not happen.
      return true;
    }

    if (input.car.position.z < 30) {
      // Dodge
      output.withJump()
          .withPitch(-1)
          .withYaw(-1)
          .withSlide();
      return true;
    }

    return false;
  }

  private static final double IDEAL_YAW = .50f;
  private static final double IDEAL_ROLL = .35f;

  private int tiltTicks = 0;
  private boolean tilt(DataPacket input, ControlsOutput output) {
    if (tiltTicks > 4 && input.car.hasWheelContact) { // Wait a few ticks to make sure we catch the jump.
      // This should not happen.
      tiltTicks = 0;
      return true;
    }

    // Check face relative to direction of travel....
    Vector3 noseVector = input.car.orientation.noseVector;
    Vector3 normalVelocity = input.car.velocity.normalized();
    double travelOffset = noseVector.flatten().correctionAngle(normalVelocity.flatten());

    boolean hasYaw = false;
    if (travelOffset < .15) {
      if (IDEAL_YAW - travelOffset > .3) {
        output.withYaw(1.0f);
      } else {
        output.withYaw(.4f);
      }
    } else if (travelOffset > .9) {
      if (travelOffset - IDEAL_YAW > .3) {
        output.withYaw(-1.0f);
      } else {
        output.withYaw(-.4f);
      }
    } else {
      hasYaw = true;
    }

    boolean hasRoll = false;
    if (input.car.orientation.rightVector.z > -.2) { // TODO: Adjust these based on how much you want to go.
      output.withRoll(.4f);
    } else if (input.car.orientation.rightVector.z < -.5) {
      output.withRoll(-.4f);
    } else {
      hasRoll = true;
    }

    boolean hasPitch = false;
    if (noseVector.z < .05) {
      output.withPitch(.4f);
    } else if (noseVector.z > .15) {
      output.withPitch(-.2f);
    } else {
      hasPitch = true;
    }

    tiltTicks++;
    if (hasRoll && hasYaw && hasPitch) {
      tiltTicks = 0;
      return true;
    }
    return false;
  }

  private int hasJumpedTicks;
  private boolean jump(DataPacket input, ControlsOutput output) {
    if (hasJumpedTicks == 0) {
      output.withJump();
    } else {
      if (hasJumpedTicks > 4) { // Incase we somehow get stuck here. Don't wait forever.
        hasJumpedTicks = 0;
      }

      // Wait until we are in the air for sure.
      if (!input.car.hasWheelContact) {
        hasJumpedTicks = 0;
        return true;
      }
    }

    hasJumpedTicks++;
    return false;
  }

  private double travelOffset(DataPacket input) {
    return input.car.orientation.noseVector.flatten().correctionAngle(input.car.velocity.normalized().flatten());
  }

  private boolean preJump(DataPacket input, ControlsOutput output) {
    output.withThrottle(1.0f);
    return input.car.groundSpeed > 800;
  }
}
