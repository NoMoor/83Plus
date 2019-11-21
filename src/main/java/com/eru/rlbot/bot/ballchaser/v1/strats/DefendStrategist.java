package com.eru.rlbot.bot.ballchaser.v1.strats;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.ballchaser.v1.tactics.RotateTactician;
import com.eru.rlbot.bot.ballchaser.v1.tactics.Tactic;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.common.DllHelper;
import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.common.PredictionUtils;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.flat.BallPrediction;
import rlbot.flat.Physics;
import rlbot.flat.PredictionSlice;

import java.util.Optional;

/** Responsible for shadowing, blocking, shots, and clearing. */
public class DefendStrategist extends Strategist {

  DefendStrategist(EruBot bot) {
    super(bot);
  }

  static boolean shouldDefend(DataPacket input) {
    return lastManBack(input) || shotOnGoal(input);
  }

  private static boolean lastManBack(DataPacket input) {
    // Check how many people are on your team.
    int myTeam = input.car.team;
    // TODO: Do something else here.

    // Check if ball is closer to your goal than you.
    Vector3 centerGoal = Goal.ownGoal(myTeam).center;
    double carToGoal = input.car.position.distance(centerGoal);
    double ballToGoal = input.ball.position.distance(centerGoal);

    return carToGoal > ballToGoal;
  }

  private static boolean shotOnGoal(DataPacket input) {
    Optional<BallPrediction> ballPredictionOptional = DllHelper.getBallPrediction();
    if (!ballPredictionOptional.isPresent()) {
      return false;
    }

    float ownGoalY = Goal.ownGoal(input.car.team).center.y;
    float inGoalY = ownGoalY + Math.signum(ownGoalY) * Constants.BALL_RADIUS;
    final BallPrediction ballPrediction = ballPredictionOptional.get();

    int i = 0;
    while (i < ballPrediction.slicesLength()) {
      Physics ballPhysics = ballPrediction.slices(i).physics();
      float ballY = ballPhysics.location().y();

      if (ownGoalY > 0 ? inGoalY < ballY : inGoalY > ballY) {
        return true;
      }

      i += 30;
    }

    return false;
  }

  private boolean shadow(DataPacket input) {
    Optional<PredictionSlice> ballInGoalOptional = PredictionUtils.getBallInGoalSlice();
    if (!ballInGoalOptional.isPresent()) {
      // TODO: Return true if the ball is being dribbled at the goal.
      return false;
    }

    Vector3 scoreLocation = Vector3.of(ballInGoalOptional.get().physics().location());
    return scoreLocation.distance(input.ball.position) < scoreLocation.distance(input.car.position);
  }

  @Override
  public boolean assign(DataPacket input) {
    if (tacticManager.isTacticLocked()) {
      // Let the tactic finish it's motion.
      return true;
    }

    // TODO: Update this to go after the ball if needed...
    if (shotOnGoal(input)) {
      if (shadow(input)) {
        tacticManager.setTactic(new Tactic(PredictionUtils.getFirstHittableBall(input.car, input.ball), Tactic.Type.SHADOW));
      } else {
        tacticManager.setTactic(new Tactic(input.car.position, Tactic.Type.DEFEND));
      }
    } else if (RotateTactician.shouldRotateBack(input)) {
      // Rotate back between the ball and the goal

      Vector3 ballToGoal = Goal.ownGoal(bot.team).center.minus(input.ball.position);
      double rotationNorm = Math.min(ballToGoal.flatten().norm(), 1500);
      Vector3 rotationTarget = input.ball.position.plus(ballToGoal.scaledToMagnitude(rotationNorm)).addX(400);

      tacticManager.setTactic(new Tactic(rotationTarget, Tactic.Type.ROTATE));
    } else {
      tacticManager.setTactic(new Tactic(input.car.position, Tactic.Type.DEFEND));
    }

    return true;
  }

  @Override
  public Strategy.Type getType() {
    return Strategy.Type.DEFEND;
  }
}
