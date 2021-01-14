package com.eru.rlbot.bot.strats;

import com.eru.rlbot.bot.common.Accels;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
import com.eru.rlbot.bot.prediction.BallPrediction;
import com.eru.rlbot.bot.prediction.BallPredictionUtil;
import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.BallData;
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

    Goal ownGoal = Goal.ownGoal(input.car.team);
    // Check time for us to get back to goal and where the ball will be then.
    double distanceToGoal = input.car.position.distance(ownGoal.center);
    Accels.AccelResult estimatedTimeToGoal =
        Accels.boostedTimeToDistance(input.car.boost, input.car.groundSpeed, distanceToGoal);
    double timeAtArrival = input.car.elapsedSeconds + estimatedTimeToGoal.getDuration();
    BallData ballAtArrival = BallPredictionUtil.get(input.car).getPredictions().stream()
        .filter(ballPrediction -> ballPrediction.ball.time > timeAtArrival)
        .findFirst()
        .map(BallPrediction::getBall)
        .orElse(input.ball);


    Vector3 ownGoalPost = Goal.ownGoal(input.car.team).getFarPost(ballAtArrival.position);

    float x, y;
    if (Math.signum(ownGoalPost.y) == Math.signum(ballAtArrival.position.y)) {
      // Ball is on our side of the field so rotate closer to goal.

      y = Math.signum(ownGoalPost.y) * (Constants.HALF_LENGTH - 1000);
      x = Math.signum(ownGoalPost.x) * ((Constants.GOAL_WIDTH / 2f) + 500);
    } else {
      y = Math.signum(ownGoalPost.y) * 3000;
      x = Math.signum(ownGoalPost.x) * 3000;
    }

    Vector3 rotationLocation = Vector3.fieldLevel(x, y);

    boolean ballNearOtherNet = Math.signum(input.ball.position.y) != Math.signum(ownGoalPost.y);

    if (!rotations.isLastManBack() && (input.car.boost > 80 || input.car.groundSpeed > 2000) && ballNearOtherNet) {
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
    Rotations rotations = Rotations.get(input);

    return (input.car.position.distance(Goal.ownGoal(input.car.team).center) < 2000 || rotations.isLastManBack())
        && !tacticManager.isTacticLocked();
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
