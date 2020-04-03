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
    Vector3 ownGoal = Goal.ownGoal(input.car.team).center;
    Vector3 otherGoal = Goal.opponentGoal(input.car.team).center;

    tacticManager.setTactic(
        Tactic.builder()
            .setTacticType(Tactic.TacticType.ROTATE)
            .setSubject(Moment.from(ownGoal))
            .setObject(otherGoal)
            .build());

    return true;
  }

  @Override
  public boolean isComplete(DataPacket input) {
    return input.car.position.distance(Goal.ownGoal(input.car.team).center) < 2000;
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
