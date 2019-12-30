package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.common.Matrix3;
import com.eru.rlbot.bot.main.Agc;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;
import java.awt.*;

/** Manages doing aerials. */
public class AerialTactician extends Tactician {

  private static final double P_GAIN = 0.1;
  private static final double I_GAIN = -0.03;
  private static final double NEG_I_GAIN = I_GAIN * ((Constants.BOOSTED_ACCELERATION + 40)/ Constants.GRAVITY);

  private static final double X_P_GAIN = 0.001;
  private static final double X_I_GAIN = 0.003;

  private static final ImmutableList<Float> HEIGHTS = ImmutableList.of(200f, 500f);
  private static final ImmutableList<Float> XS = ImmutableList.of(0f, 1000f);

  AerialTactician(Agc bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  boolean jumpLock = false;

  // TODO: Clean this up. It is very shaky.
  @Override
  public void execute(DataPacket input, ControlsOutput output, Tactic tactic) {
    Vector3 interceptLocation = tactic.subject.position.minus(tactic.object)
        .toMagnitude(Constants.BALL_RADIUS * .5)
        .plus(tactic.subject.position);

    bot.botRenderer.renderProjection(input.car, interceptLocation, Color.GRAY);

    double interceptTime = input.car.position.distance(interceptLocation) / input.car.velocity.magnitude();

    Vector3 boostVector = calculateBoost(input.car, interceptLocation, interceptTime);

    Vector3 boostIndicator = boostVector.divide(Constants.BOOSTED_ACCELERATION / 100);
    bot.botRenderer.renderProjection(input.car, input.car.position.plus(boostIndicator), boostIndicator.magnitude() < 100 ? Color.RED : Color.green);

    if (input.car.hasWheelContact) {
      output
          .withThrottle(1.0f)
          .withSteer(Angles.flatCorrectionAngle(input.car, tactic.subject.position));

      double flatDistance = tactic.subject.position.flatten().distance(input.car.position.flatten());

      if (tactic.subject.position.z * 4 / Constants.BOOSTED_ACCELERATION > flatDistance / input.car.groundSpeed) {
        tacticManager.delegateTactic(tactic, FastAerial.class);
      }
    } else if (interceptTime < .75) {
      pointAt(input.car, tactic.subject.position, output);

      double angleOffset = input.car.orientation.getNoseVector().angle(tactic.subject.position.minus(input.car.position));
      bot.botRenderer.setBranchInfo("Aim t: %f d: %f", interceptTime, angleOffset);
      if (Math.abs(angleOffset) < .1) {
        output.withBoost();
      }
    } else {
      bot.botRenderer.setBranchInfo("Fly time: %f", interceptTime);
      fly(input, boostVector, output);
    }
  }

  private void fly(DataPacket input, Vector3 boostVector, ControlsOutput output) {
    pointAt(input.car, boostVector, output);

    double sensitivity = boostVector.magnitude() * .25 / Constants.BOOSTED_ACCELERATION;
    double offset = input.car.orientation.getNoseVector().angle(boostVector);

    if (offset < sensitivity) {
      bot.botRenderer.setBranchInfo("Boosting!");
      output.withBoost();
    } else {
      bot.botRenderer.setBranchInfo("Too big of angle %f", offset);
    }
  }

  private void pointAt(CarData car, Vector3 desiredVector, ControlsOutput output) {
    Vector3 nose = desiredVector.normalize();
    Vector3 sideDoor = nose.cross(Vector3.of(0, 0, 1)).normalize();
    Vector3 roofOrientation = nose.cross(sideDoor);
    Orientation carOrientation = Orientation.noseRoof(nose, roofOrientation);

    // Draw the nose orientation.
    bot.botRenderer.renderProjection(
        car,
        car.position.plus(carOrientation.getNoseVector().multiply(200)),
        Color.white);

    Angles3.setControlsFor(car, carOrientation.getOrientationMatrix(), output);
  }

  private Vector3 calculateBoost(CarData car, Vector3 interceptLocation, double interceptTime) {
    // Assume we will boost for 1/2 of the time
    double boostTime = interceptTime * .9;
    double coefficient = 2 / (interceptTime * (interceptTime - boostTime));

    return interceptLocation
        .minus(car.position)
        .minus(car.velocity.multiply(interceptTime))
        .minus(Vector3.of(0, 0, Constants.NEG_GRAVITY).multiply(.5 * interceptTime))
        .multiply(coefficient);
  }

  private void executeAerialPD(DataPacket input, ControlsOutput output, Tactic nextTactic) {
    if (input.car.hasWheelContact || jumpLock) {
      output.withJump();
      jumpLock = !JumpManager.hasMaxJumpHeight();
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

    Matrix3 target = Orientation.convert(Math.PI / 2 + pitchTarget, 0, 0).getOrientationMatrix();

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
