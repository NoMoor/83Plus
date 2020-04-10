package com.eru.rlbot.bot.strats;

import com.eru.rlbot.bot.common.Constants;
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

    Rotations rotations = Rotations.get(input);

    Vector3 ownGoal = Goal.ownGoal(input.car.team).getFarPost(input.ball.position);

    float x, y;
    if (Math.signum(ownGoal.y) == Math.signum(input.ball.position.y)) {
      // Ball is on our side of the field

      y = Math.signum(ownGoal.y) * (Constants.HALF_LENGTH - 1000);
      x = Math.signum(ownGoal.x) * ((Constants.GOAL_WIDTH / 2f) + 500);
    } else {
      y = Math.signum(ownGoal.y) * 3000;
      x = Math.signum(ownGoal.x) * 3000;
    }

    Vector3 rotationLocation = Vector3.fieldLevel(x, y);
    if (!rotations.isLastManBack() && (input.car.boost > 80 || input.car.groundSpeed > 2000)) {
      tacticManager.setTactic(
          Tactic.builder()
              .setTacticType(Tactic.TacticType.DEMO)
              .setSubject(rotationLocation)
              .build());
    } else {
      tacticManager.setTactic(
          Tactic.builder()
              .setTacticType(Tactic.TacticType.ROTATE)
              .setSubject(Moment.from(rotationLocation))
              .setObject(input.ball.position)
              .build());
    }

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
