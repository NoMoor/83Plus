package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.common.*;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;

public class RotateTactician extends Tactician {

  private boolean flipLock;

  RotateTactician(EruBot bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  public static boolean shouldRotateBack(DataPacket input) {
    double carToGoal = input.car.position.distance(Goal.ownGoal(input.car.team).center);
    double ballToGoal = input.ball.position.distance(Goal.ownGoal(input.car.team).center);

    return ballToGoal < carToGoal;
  }

  @Override
  public boolean isLocked() {
    return flipLock;
  }

  @Override
  void execute(DataPacket input, ControlsOutput output, Tactic tactic) {
    if (flipLock) {
      flip(input, output, tactic.targetMoment.position);
    } else {
      moveTowardTarget(input, output, tactic.targetMoment.position);
    }
  }

  private void moveTowardTarget(DataPacket input, ControlsOutput output, Vector3 nextTargetPoint) {

    // How far to rotate the car to see the ball
    double correctionAngle = Angles.flatCorrectionDirection(input.car, input.ball.position);

    bot.botRenderer.setBranchInfo("Rotate %f", correctionAngle);

    Vector3 correctedTarget = nextTargetPoint.plus(getTargetOffset(input.car, nextTargetPoint, correctionAngle));
    double correctionDirection = Angles.flatCorrectionDirection(input.car, correctedTarget);

    bot.botRenderer.setCarTarget(correctedTarget);

    output.withSteer(correctionDirection * STEERING_SENSITIVITY);

    output.withThrottle(1);
    double distanceToTarget = input.car.position.distance(nextTargetPoint);

    if (input.car.hasWheelContact && distanceToTarget > 1000 && (input.car.boost > 50 || Angles.isRotatingBack(input))) {
      output.withBoost();
    }

    if (distanceToTarget > 1000 && correctionAngle < .5 && input.car.velocity.norm() > 1400) {
      flipLock = true;
      output.withJump();
    } else if (distanceToTarget < 100) {
      output.withThrottle(-1f);
    }
  }

  private static final double P_GAIN = 15d * -Constants.BALL_RADIUS;
  private static final double D_GAIN = .2d;
  private static final double STEERING_SENSITIVITY = 15d;
  private Vector3 getTargetOffset(CarData car, Vector3 target, double correctionAngle) {
    double xCorrection = Math.sin(correctionAngle) * P_GAIN;
    double xDampeningCorrection = -Math.sin(correctionAngle) * car.velocity.flatten().norm() * D_GAIN;

    double yCorrection = -Constants.BALL_RADIUS;

    // Rotate to be relative to where the car is.
    Vector3 carBall = Angles.carTarget(car, target);
    double rotationAngle = carBall.flatten().correctionAngle(Vector2.NORTH);
    return Angles.rotate(new Vector2(xCorrection + xDampeningCorrection, yCorrection), rotationAngle).asVector3();
  }

  private int jumpTicks;
  private void flip(DataPacket input, ControlsOutput output, Vector3 nextTargetPoint) {
    double correctionAngle = Angles.flatCorrectionDirection(input.car, nextTargetPoint);

    output.withThrottle(1.0f);

    if (!JumpManager.hasReleasedJumpInAir()) {
      // Prevent getting stuck
      if (jumpTicks++ > 20 && input.car.hasWheelContact) {
        flipLock = false;
      }
    } else if (JumpManager.canFlip()) {
      jumpTicks = 0;
      // TODO: Check ball location before flipping.
      output
          .withYaw(-Math.signum(correctionAngle) * .5)
          .withPitch(-1)
          .withJump();
    } else {
      // TODO: Update this to be flat facing in the direction of travel.
//      Angles3.setControlsFor(input.car, Matrix3.IDENTITY, output);
      output.withThrottle(1.0f);
      if (input.car.hasWheelContact) {
        flipLock = false;
      }
    }
  }
}
