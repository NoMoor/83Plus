package com.eru.rlbot.bot.ballchaser.v1.strats;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.ballchaser.v1.tactics.*;
import com.eru.rlbot.bot.common.*;
import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;

/** Responsible for dribbling, shooting, and passing. */
public class AttackStrategist extends Strategist {

  AttackStrategist(EruBot bot) {
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
              .plan(planner::plan));
      return true;
    }

    if (TakeTheShotTactician.takeTheShot(input)) {
      Moment targetMoment = TakeTheShotTactician.shotTarget(input);
      tacticManager.setTactic(
          Tactic.builder()
              .setSubject(targetMoment)
              .setTacticType(Tactic.TacticType.STRIKE)
              .plan(planner::plan));
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
