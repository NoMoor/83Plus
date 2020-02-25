package com.eru.rlbot.bot.strats;

import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.common.PredictionUtils;
import com.eru.rlbot.bot.main.Agc;
import com.eru.rlbot.bot.tactics.AerialTactician;
import com.eru.rlbot.bot.tactics.KickoffTactician;
import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.bot.tactics.TakeTheShotTactician;
import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.DataPacket;

/** Responsible for dribbling, shooting, and passing. */
public class AttackStrategist extends Strategist {

  AttackStrategist(Agc bot) {
    super(bot);
  }

  @Override
  public boolean assign(DataPacket input) {
    // TODO: Use ball prediction util to plan shots/passes/aerials. Then pick the first one that is reachable and
    // create the tactic.

    if (tacticManager.isTacticLocked()) {
      // Let the tactic finish it's motion.
      return true;
    }

    if (KickoffTactician.isKickOff(input)) {
      tacticManager.setTactic(
          Tactic.builder()
              .setSubject(Moment.from(input.ball))
              .setTacticType(Tactic.TacticType.KICKOFF)
              .build());
      return true;
    }

    // TODO: Update when the opponent can get to the ball.

    // TODO: Pick a goal shot.
    AerialTactician.doAerialPlanning(input);

    // Execute that shot.
    BallPredictionUtil.ExaminedBallData ballToHit = BallPredictionUtil.forCar(input.car).getFirstHittableLocation();

    if (ballToHit != null) {
      tacticManager.setTactic(Tactic.builder()
          .setSubject(Moment.from(ballToHit))
          .setTacticType(ballToHit.getTactic())
          .setObject(Goal.opponentGoal(input.car.team).center)
          .build());
      return true;
    }

//    if (input.ball.position.z > 300) {
//      tacticManager.setTactic(Tactic.builder()
//          .setSubject(Moment.from(input.ball))
//          .setObject(Goal.opponentGoal(input.car.team).center)
//          .setTacticType(Tactic.TacticType.AERIAL)
//          .build());
//      return true;
//    }

    if (TakeTheShotTactician.takeTheShot(input)) {
      tacticManager.setTactic(
          Tactic.builder()
              .setObject(Goal.opponentGoal(input.car.team).center)
              .setTacticType(Tactic.TacticType.STRIKE)
              .build());
      return true;
    }

    tacticManager.setTactic(Tactic.builder()
        .setSubject(PredictionUtils.getFirstHittableBall(input))
        .setTacticType(Tactic.TacticType.HIT_BALL)
        .plan(new PathPlanner(input)::plan));
    return true;
  }

  @Override
  public Strategy.Type getType() {
    return Strategy.Type.ATTACK;
  }
}
