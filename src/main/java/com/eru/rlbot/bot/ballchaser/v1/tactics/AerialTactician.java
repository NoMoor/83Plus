package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.common.Matrix3;
import com.eru.rlbot.common.input.CarOrientation;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.ControlsOutput;
import com.google.common.collect.ImmutableList;

/** Manages doing aerials. */
public class AerialTactician extends Tactician {

  private static final double P_GAIN = 0.1;
  private static final double I_GAIN = -0.03;
  private static final double NEG_I_GAIN = I_GAIN * ((Constants.BOOSTED_ACCELERATION + 40)/ Constants.GRAVITY);

  private static final double X_P_GAIN = 0.001;
  private static final double X_I_GAIN = 0.003;

  private static final ImmutableList<Float> HEIGHTS = ImmutableList.of(200f, 500f);
  private static final ImmutableList<Float> XS = ImmutableList.of(0f, 1000f);

  AerialTactician(EruBot bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  boolean jumpLock = false;

  @Override
  public void execute(DataPacket input, ControlsOutput output, Tactic nextTactic) {
    if (input.car.hasWheelContact || jumpLock) {
      output.withJump();
      jumpLock = true;
      if (JumpManager.hasMaxJumpHeight()) {
        jumpLock = false;
      }
    }

    // Positive if we need to go up.
    double deltaZ = getTargetHeight(input.car.elapsedSeconds) - input.car.position.z;
    double deltaZt = input.car.velocity.z;

    double boostAction = deltaZ * P_GAIN + deltaZt * (deltaZt > 0 ? I_GAIN : NEG_I_GAIN);

    if (boostAction > 0) {
      output.withBoost();
    }

    double deltaX = input.car.position.x - getTargetX(input.car.elapsedSeconds);
    double deltaXt = input.car.velocity.x;

    double pitchTarget = deltaX * X_P_GAIN + deltaXt * X_I_GAIN;

    bot.botRenderer.setBranchInfo("X: %f Pitch: %f", deltaX, pitchTarget);

    Matrix3 target = CarOrientation.convert(Math.PI / 2 + pitchTarget, 0, 0).getOrientationMatrix();

    Angles3.setControlsFor(input.car, target, output);
  }

  private double getTargetX(float time) {
    int index = (((int) time) % 20) / 10;
    return XS.get(index);
  }

  private double getTargetHeight(float time) {
    int index = (((int) time) % 20) / 10;
    return HEIGHTS.get(index);
  }
}
