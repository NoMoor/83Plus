package com.eru.rlbot.bot.ballchaser.v1.strats;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.ballchaser.v1.tactics.*;
import com.eru.rlbot.bot.common.*;
import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.flat.BallPrediction;
import java.util.Optional;

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

    if (KickoffTactician.isKickOff(input)) {
      tacticManager.setTactic(new Tactic(input.ball.position, Tactic.Type.KICKOFF));
      return true;
    }

    if (TakeTheShotTactician.takeTheShot(input)) {
      Moment targetMoment = TakeTheShotTactician.shotTarget(input);
      tacticManager.setTactic(new Tactic(targetMoment, Tactic.Type.STRIKE));
      return true;
    }
//    else if (DribbleTactician.canDribble(input)) {
//      tacticManager.setTactic(new Tactic(input.ball.position, Tactic.Type.DRIBBLE));
//    } else if (PickUpTactician.canPickUp(input)) {
//      tacticManager.setTactic(new Tactic(input.ball.position, Tactic.Type.PICK_UP));
//    } else if (CatchTactician.canCatch(input)) {
//      tacticManager.setTactic(new Tactic(input.ball.position, Tactic.Type.CATCH));
//    } else {
//      tacticManager.setTactic(new Tactic(input.ball.position, Tactic.Type.HIT_BALL));
//    }

    tacticManager.setTactic(new Tactic(PredictionUtils.getFirstHittableBall(input), Tactic.Type.HIT_BALL));
    return true;
  }

  @Override
  public Strategy.Type getType() {
    return Strategy.Type.ATTACK;
  }
}
