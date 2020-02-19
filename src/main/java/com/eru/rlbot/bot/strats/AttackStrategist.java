package com.eru.rlbot.bot.strats;

import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.common.PredictionUtils;
import com.eru.rlbot.bot.main.Agc;
import com.eru.rlbot.bot.tactics.KickoffTactician;
import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.bot.tactics.TakeTheShotTactician;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;

/** Responsible for dribbling, shooting, and passing. */
public class AttackStrategist extends Strategist {

  AttackStrategist(Agc bot) {
    super(bot);
  }

  public static boolean shouldAttack(DataPacket input) {
    return !ballIsFarCorner(input);
  }

  private static boolean ballIsFarCorner(DataPacket input) {
    Vector3 oppGoalCenter = Goal.opponentGoal(input.car.team).center;
    Vector3 ballPosition = input.ball.position;

    Vector3 ballToGoal = oppGoalCenter.minus(ballPosition);

    // TODO: Specify this better.
    return Math.abs(ballToGoal.x) > Math.abs(ballToGoal.y) + 2000;
  }

  @Override
  public boolean assign(DataPacket input) {
    if (tacticManager.isTacticLocked()) {
      // Let the tactic finish it's motion.
      return true;
    }

    PathPlanner planner = new PathPlanner(input);

    if (KickoffTactician.isKickOff(input)) {
      tacticManager.setTactic(
          Tactic.builder()
              .setSubject(input.ball.position)
              .setTacticType(Tactic.TacticType.KICKOFF)
              .build());
      return true;
    }

    if (false) {
      tacticManager.setTactic(
          Tactic.builder()
              .setSubject(input.ball.position)
              .setTacticType(Tactic.TacticType.JUMP_FLIP)
              .build());
      return true;
    }

    // TODO: Figure out when to invoke the aerial stuff.
    if (input.ball.position.z > 300) {
      tacticManager.setTactic(Tactic.ballTactic()
          .setSubject(input.ball.position)
          .setObject(Goal.opponentGoal(input.car.team).center)
          .setTacticType(Tactic.TacticType.AERIAL)
          .build());
      return true;
    }

    if (TakeTheShotTactician.takeTheShot(input)) {
      tacticManager.setTactic(
          Tactic.builder()
              .setObject(Goal.opponentGoal(input.car.team).center)
              .setTacticType(Tactic.TacticType.STRIKE)
              .build());
      return true;
    }
//    else if (DribbleTactician.canDribble(input)) {
//      tacticManager.setTactic(new Tactic(input.ball.position, Tactic.TacticType.DRIBBLE));
//    } else if (PickUpTactician.canPickUp(input)) {
//      tacticManager.setTactic(new Tactic(input.ball.position, Tactic.TacticType.PICK_UP));
//    } else if (CatchTactician.canCatch(input)) {
//      tacticManager.setTactic(new Tactic(input.ball.position, Tactic.TacticType.CATCH));
//    } else {
//      tacticManager.setTactic(new Tactic(input.ball.position, Tactic.TacticType.HIT_BALL));
//    }

    tacticManager.setTactic(Tactic.builder()
        .setSubject(PredictionUtils.getFirstHittableBall(input))
        .setTacticType(Tactic.TacticType.HIT_BALL)
        .plan(planner::plan));
    return true;
  }

  @Override
  public Strategy.Type getType() {
    return Strategy.Type.ATTACK;
  }
}
