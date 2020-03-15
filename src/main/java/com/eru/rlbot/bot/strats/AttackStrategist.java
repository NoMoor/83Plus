package com.eru.rlbot.bot.strats;

import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
import com.eru.rlbot.bot.plan.Marker;
import com.eru.rlbot.bot.prediction.BallPrediction;
import com.eru.rlbot.bot.prediction.BallPredictionUtil;
import com.eru.rlbot.bot.tactics.KickoffTactician;
import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.DataPacket;

/**
 * Responsible for dribbling, shooting, and passing.
 */
public class AttackStrategist extends Strategist {

  AttackStrategist(ApolloGuidanceComputer bot) {
    super(bot);
  }

  private long index;

  @Override
  public boolean assign(DataPacket input) {
    // TODO: Use ball prediction util to plan shots/passes/aerials. Then pick the first one that is reachable and
    // create the tactic.

    if (tacticManager.isTacticLocked()) {
      // Let the tactic finish it's motion.
      return true;
    }

    if (KickoffTactician.isKickoffStart(input)) {
      tacticManager.setTactic(
          Tactic.builder()
              .setSubject(Moment.from(input.ball))
              .setTacticType(Tactic.TacticType.KICKOFF)
              .build());
      return true;
    }

    int carPredictionIndex = getPredictionIndex(input.allCars.size());
    Marker.get(input.serialNumber).mark(input, carPredictionIndex);

    if (false) {
      tacticManager.setTactic(Tactic.builder()
          .setSubject(Moment.from(input.ball))
          .setTacticType(Tactic.TacticType.DEMO)
          .build());
      return true;
    }

    // TODO: Update when the opponent can get to the ball.

    // TODO: Select a ball to hit, not just the first one.
    // Execute that shot.
    BallPrediction ballToHit = BallPredictionUtil.get(input.car).getTarget();

    if (ballToHit != null) {
      tacticManager.setTactic(Tactic.builder()
          .setSubject(Moment.from(ballToHit))
          .setTacticType(ballToHit.getTactic())
          .setObject(Goal.opponentGoal(input.car.team).center)
          .build());
      return true;
    }

    return false;
  }

  private int getPredictionIndex(int size) {
    return (int) (index++ % size);
  }

  @Override
  public Strategy.Type getType() {
    return Strategy.Type.ATTACK;
  }
}
