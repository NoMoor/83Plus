package com.eru.rlbot.bot.strats;

import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;

public class RotateStrategist extends Strategist {

  public RotateStrategist(ApolloGuidanceComputer bot) {
    super(bot);
  }

  @Override
  boolean assign(DataPacket input) {
    if (tacticManager.isTacticLocked()) {
      // Let the tactic finish it's motion.
      return true;
    }

    Vector3 ownGoal = Goal.ownGoal(input.car.team).getFarPost(input.ball.position);
    ownGoal = ownGoal.setY(Math.signum(ownGoal.y) * 4000);

    tacticManager.setTactic(
        Tactic.builder()
            .setTacticType(Tactic.TacticType.ROTATE)
            .setSubject(Moment.from(ownGoal))
            .setObject(input.ball.position)
            .build());

    return true;
  }

  @Override
  public boolean isComplete(DataPacket input) {
    return input.car.position.distance(Goal.ownGoal(input.car.team).center) < 3000;
  }

  @Override
  public Strategy.Type getType() {
    return Strategy.Type.ROTATE;
  }

  @Override
  public Strategy.Type getDelegate() {
    return Strategy.Type.DEFEND;
  }
}
