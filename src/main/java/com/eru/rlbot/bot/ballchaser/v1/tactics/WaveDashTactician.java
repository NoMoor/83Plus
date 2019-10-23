package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.common.Matrix3;
import com.eru.rlbot.common.input.DataPacket;
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

    bot.botRenderer.setBranchInfo(currentStage.name());

    boolean stageComplete = doCurrentStage(input, output);
    // Hold slide if we are traveling at all sideways.
    output.withSlide(input.car.groundSpeed > 800 && travelOffset(input) > .05);

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
        return tilt2(input, output);
//        return tilt(input, output);
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

    if (input.car.hasWheelContact && slideTicks > 1) {
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
          .withPitch(-.8)
          .withYaw(-1)
          .withSlide();
      return true;
    }

    return false;
  }

  private static final float IDEAL_PITCH = .1f;
  private static final float IDEAL_ROLL = -.35f;
  private static final float IDEAL_YAW = .6f;
  private static final float IDEAL_YAW2 = .5f;

  private int tiltTicks = 0;

  private boolean tilt2(DataPacket input, ControlsOutput output) {
    if (tiltTicks > 10 && input.car.hasWheelContact) { // Wait a few ticks to make sure we catch the jump.
      // This should not happen.
      tiltTicks = 0;
      return true;
    }

    Matrix3 target = Matrix3.of(Vector3.of(1, 0, 0), Vector3.of(0, -1, 0), Vector3.of(0, 0, 1));

    Angles3.makeControlsFor(input.car, target, output);

    // Max out controls.
    float pitchInput = output.getPitch();
    float yawInput = output.getYaw();
    float rollInput = output.getRoll();

    // Normalize the values keeping the sign.
    float maxValue = Math.max(Math.abs(pitchInput), Math.max(Math.abs(rollInput), Math.abs(yawInput)));
    output.withPitch(pitchInput / maxValue);
    output.withRoll(rollInput / maxValue);
    output.withYaw(yawInput / maxValue);

    boolean hasYaw = Math.abs(travelToTargetOffset(input) - IDEAL_YAW2) < (IDEAL_YAW2 / 5);
    boolean hasPitch = Math.abs(input.car.orientation.getNoseVector().z - IDEAL_PITCH) < (IDEAL_PITCH / 5);
    boolean hasRoll = Math.abs(input.car.orientation.getRightVector().z - IDEAL_ROLL) < (IDEAL_ROLL / 5);

    tiltTicks++;
    if (hasRoll && hasYaw && hasPitch) { // Get them all at the same time.
      tiltTicks = 0;
      return true;
    }
    return false;
  }

  private boolean tilt(DataPacket input, ControlsOutput output) {
    if (tiltTicks > 4 && input.car.hasWheelContact) { // Wait a few ticks to make sure we catch the jump.
      // This should not happen.
      tiltTicks = 0;
      return true;
    }

    // Check face relative to direction of travel....
    Vector3 noseVector = input.car.orientation.getNoseVector();
    Vector3 normalTarget = currentTactic.target.position.minus(input.car.position); // Draw a vector between our positions.
    double travelOffset = noseVector.flatten().correctionAngle(normalTarget.flatten());

    bot.botRenderer.setBranchInfo(String.format("%#2f", travelOffset));

    boolean hasYaw = false;
    if (travelOffset < IDEAL_YAW - .10) {
      if (IDEAL_YAW - travelOffset > .6) {
        output.withYaw(.1f);
      } else if (IDEAL_YAW - travelOffset > .2) {
        output.withYaw(.4f);
      } else {
        output.withYaw(.2f);
      }
    } else if (travelOffset > IDEAL_YAW + .10) {
      if (travelOffset - IDEAL_YAW > .6) {
        output.withYaw(-.1f);
      } else if (travelOffset - IDEAL_YAW > .2) {
        output.withYaw(-.4f);
      } else {
        output.withYaw(-.2f);
      }
    } else {
      hasYaw = true;
    }

    double rollZ = input.car.orientation.getRightVector().z;
    boolean hasRoll = false;
    if (rollZ > IDEAL_ROLL + .05) { // TODO: Adjust these based on how much you want to go.
      if (rollZ - IDEAL_ROLL > .4) {
        output.withRoll(.7f);
      } else {
        output.withRoll(.3f);
      }
    } else if (rollZ < IDEAL_ROLL - .05) {
      if (IDEAL_ROLL - rollZ > .4) {
        output.withRoll(-.7f);
      } else {
        output.withRoll(-.3f);
      }
    } else {
      hasRoll = true;
    }

    boolean hasPitch = false;
    if (noseVector.z < .05) {
      output.withPitch(.4f);
    } else if (noseVector.z > .15) {
      output.withPitch(-.4f);
    } else {
      hasPitch = true;
    }

    tiltTicks++;
    if (hasRoll || hasPitch) {
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
      // Wait until we are in the air for sure.
      if (!input.car.hasWheelContact) {
        hasJumpedTicks = 0;
        return true;
      }
    }

    hasJumpedTicks++;
    if (hasJumpedTicks > 2) { // Incase we somehow get stuck here. Don't wait forever.
      hasJumpedTicks = 0;
    }

    return false;
  }

  private double travelToTargetOffset(DataPacket input) {
    return input.car.orientation.getNoseVector().flatten().correctionAngle(currentTactic.getTarget().flatten());
  }

  private double travelOffset(DataPacket input) {
    return input.car.orientation.getNoseVector().flatten().correctionAngle(input.car.velocity.normalized().flatten());
  }

  private boolean preJump(DataPacket input, ControlsOutput output) {
    output.withThrottle(1.0f);
    output.withSteer(Angles.flatCorrectionDirection(input.car, currentTactic.target.position) + .23);
    return input.car.groundSpeed > 1200;
  }
}
