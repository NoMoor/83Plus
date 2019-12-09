package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.main.Agc;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;

public class RotateTactician extends Tactician {

  RotateTactician(Agc bot, TacticManager tacticManager) {
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

  private boolean locked;
  @Override
  public boolean isLocked() {
    return locked;
  }

  private void moveTowardTarget(DataPacket input, ControlsOutput output, Tactic tactic) {
    bot.botRenderer.setBranchInfo("Rotating for %s", tactic.subjectType);

    Vector3 nextTargetPoint = tactic.subject.position;

    double correctionDirection = Angles.flatCorrectionAngle(input.car, nextTargetPoint);

    output.withSteer(correctionDirection);

    // TODO: Perhaps slow down...
    output.withThrottle(1);
    double distanceToTarget = input.car.position.distance(nextTargetPoint);

    if (input.car.hasWheelContact
        && !input.car.isSupersonic
        && (input.car.boost > 20 || Angles.isRotatingBack(input))) {
      output.withBoost();
    }

    if (!input.car.hasWheelContact) {
      Angles3.setControlsForFlatLanding(input.car, output);
    }

    if (distanceToTarget < 50) {
      tacticManager.setTacticComplete(tactic);
    } else if (distanceToTarget < 500 && Math.abs(correctionDirection) > 1) {
      output.withSlide();
    } else if (((distanceToTarget > 1500 && nextTargetPoint.z < 150) || distanceToTarget > 2500) // Distance
        && ((input.car.groundSpeed > 1300 && input.car.boost < 20) || input.car.groundSpeed > 1500) // Speed
        && !input.car.isSupersonic // Excess Speed
        && Math.abs(correctionDirection) < .25) { // Angle
      tacticManager.preemptTactic(Tactic.builder()
          .setSubject(nextTargetPoint)
          .setTacticType(Tactic.TacticType.FLIP)
          .build());
    }
  }
}
