package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.common.*;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;

public class RotateTactician extends Tactician {

  RotateTactician(EruBot bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  public static boolean shouldRotateBack(DataPacket input) {
    double carToGoal = input.car.position.distance(Goal.ownGoal(input.car.team).center);
    double ballToGoal = input.ball.position.distance(Goal.ownGoal(input.car.team).center);

    return ballToGoal < carToGoal;
  }

  @Override
  void execute(DataPacket input, ControlsOutput output, Tactic tactic) {
    moveTowardTarget(input, output, tactic);
  }

  private void moveTowardTarget(DataPacket input, ControlsOutput output, Tactic tactic) {
    Vector3 nextTargetPoint = tactic.targetMoment.position;


    double correctionDirection = getTargetOffset(input, nextTargetPoint, tactic);

    output.withSteer(correctionDirection * STEERING_SENSITIVITY);

    // TODO: Perhaps slow down...
    output.withThrottle(1);
    double distanceToTarget = input.car.position.distance(nextTargetPoint);

    if (input.car.hasWheelContact && input.car.groundSpeed < Constants.SUPER_SONIC && (input.car.boost > 50 || Angles.isRotatingBack(input))) {
      output.withBoost();
    }

    if (distanceToTarget < 100) {
      tacticManager.setTacticComplete(tactic);
    }
  }

  private static final double P_GAIN = .4d;
  private static final double D_GAIN = -.0001d;
  private static final double STEERING_SENSITIVITY = 15d;
  private double getTargetOffset(DataPacket input, Vector3 target, Tactic tactic) {
    double targetNoseBall = Angles.flatCorrectionAngle(
        tactic.targetMoment.position, input.car.orientation.getNoseVector(), input.ball.position);

    double carTargetCorrection = Angles.flatCorrectionDirection(input.car, target);

    double xCorrection = -targetNoseBall * P_GAIN;

    Vector3 carTargetVector = tactic.targetMoment.position.minus(input.car.position);
    double carTargetBallCorrection =
        Angles.flatCorrectionAngle(tactic.targetMoment.position, carTargetVector, input.ball.position);
    double projectedCorrection = carTargetBallCorrection + carTargetCorrection;

    double attackVelocity = Math.sin(Math.abs(projectedCorrection)) * input.car.groundSpeed;

    double xDampeningCorrection = attackVelocity * D_GAIN;

    double correctionAngle = carTargetCorrection + xCorrection + xDampeningCorrection;

    bot.botRenderer.setBranchInfo(Math.signum(targetNoseBall) != Math.signum(correctionAngle) ? "Swing out" : "Sweep through");
    return correctionAngle;
  }
}
