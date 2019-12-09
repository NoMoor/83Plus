package com.eru.rlbot.bot.strats;

import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.main.Agc;
import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;

/** Strategy responsible for patience. */
public class SupportStrategist extends Strategist {

  private static final Vector3 LEFT_MID = Vector3.of(3584, 0, 73);
  private static final Vector3 RIGHT_MID = Vector3.of(-3584, 0, 73);

  public SupportStrategist(Agc bot) {
    super(bot);
  }

  @Override
  public boolean assign(DataPacket input) {
    // TODO: If we are supporting, pick up boost nearby.

    PathPlanner pathPlanner = new PathPlanner(input);

    if (input.car.boost < 50) {
      Vector3 boostLocation = input.car.position.x > 0 ? LEFT_MID : RIGHT_MID;
      tacticManager.setTactic(Tactic.builder()
          .setSubject(boostLocation)
          .setTacticType(Tactic.TacticType.ROTATE)
          .plan(pathPlanner::plan));
    } else {
      // TODO: Update to work with both teams.
      Vector3 ballGoal = Goal.ownGoal(input.team).center.minus(input.ball.position);
      Vector3 rotationOffSet = ballGoal.multiply(Math.min(2000, .75 * ballGoal.norm()));
      Vector3 rotationPoint = input.ball.position.plus(rotationOffSet);
      Vector3 rotationPointToBall = input.ball.position.minus(rotationPoint);

      tacticManager.setTactic(Tactic.builder()
          .setSubject(new Moment(rotationPoint, rotationPointToBall))
          .setTacticType(Tactic.TacticType.ROTATE)
          .plan(pathPlanner::plan));
    }

    return true;
  }

  @Override
  public Strategy.Type getType() {
    return Strategy.Type.SUPPORT;
  }
}
