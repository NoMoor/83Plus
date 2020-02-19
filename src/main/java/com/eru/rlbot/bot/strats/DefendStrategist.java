package com.eru.rlbot.bot.strats;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.common.DllHelper;
import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.common.Locations;
import com.eru.rlbot.bot.common.Pair;
import com.eru.rlbot.bot.common.PredictionUtils;
import com.eru.rlbot.bot.main.Agc;
import com.eru.rlbot.bot.tactics.RotateTactician;
import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;
import java.util.Comparator;
import java.util.Optional;
import rlbot.flat.BallPrediction;
import rlbot.flat.Physics;
import rlbot.flat.PredictionSlice;

/** Responsible for shadowing, blocking, shots, and clearing. */
public class DefendStrategist extends Strategist {

  DefendStrategist(Agc bot) {
    super(bot);
  }

  static boolean shouldDefend(DataPacket input) {
    return lastManBack(input) && (ballNearGoal(input) || canClear(input));
  }

  private static boolean lastManBack(DataPacket input) {
    int myTeam = input.car.team;
    Vector3 ownGoal = Goal.ownGoal(myTeam).center;

    // TODO: Look at momentum as well as position.
    CarData lastManBack = input.allCars.stream()
        .filter(car -> car.team == myTeam)
        .map(car -> Pair.of(car, car.position.distance(ownGoal)))
        .min(Comparator.comparing(Pair::getSecond))
        .orElseThrow(IllegalStateException::new)
        .getFirst();

    return lastManBack == input.car;
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
      bot.botRenderer.addAlertText("Keep tactic");
      return true;
    }

    PathPlanner pathPlanner = new PathPlanner(input);

    // TODO: Update to include the opponent hitting the ball
    if (shotOnGoal(input)) {
      if (shadow(input)) {
        tacticManager.setTactic(Tactic.builder()
            .setSubject(PredictionUtils.getFirstHittableBall(input))
            .setTacticType(Tactic.TacticType.SHADOW)
            .build());
      } else {
        tacticManager.setTactic(Tactic.builder()
            .setSubject(input.ball.position)
            .setTacticType(Tactic.TacticType.DEFEND)
            .build());
      }
    } else if (canClear(input)) {
      if (Locations.carToBall(input).magnitude() > 2000) {
        bot.botRenderer.addAlertText("Rotate clear");
        tacticManager.setTactic(Tactic.ballTactic()
          .setSubject(PredictionUtils.getFirstHittableBall(input))
          .setTacticType(Tactic.TacticType.ROTATE)
          .build());
      } else {
        tacticManager.setTactic(Tactic.ballTactic()
            .setSubject(input.ball.position)
            .setTacticType(Tactic.TacticType.DEFEND)
            .build());
      }
    } else if (RotateTactician.shouldRotateBack(input)) {
      // Rotate far post
      Vector3 farPost = Locations.farPost(input);
      farPost = farPost.addY(-Math.signum(farPost.y) * 500);
      tacticManager.setTactic(Tactic.builder()
          .setSubject(farPost)
          .setTacticType(Tactic.TacticType.ROTATE)
          .plan(pathPlanner::plan));
    } else {
      tacticManager.setTactic(Tactic.builder()
          .setSubject(input.ball.position)
          .setTacticType(Tactic.TacticType.DEFEND)
          .build());
    }

    return true;
  }

  private static boolean canClear(DataPacket input) {
    boolean ballIsInFrontOfCar = Math.abs(Angles.flatCorrectionAngle(input.car, input.ball.position)) < .75;
    boolean ballNearGoal = ballNearGoal(input);
    boolean isPointedAwayFromGoal =
        Math.abs(Locations.minCarTargetNotGoalCorrection(input, new Moment(input.ball, input.car.elapsedSeconds))) < .2;

    return ballIsInFrontOfCar && ballNearGoal && isPointedAwayFromGoal;
  }

  private static double GOAL_NEARNESS_THRESHOLD = Constants.GOAL_WIDTH * 2;
  private static boolean ballNearGoal(DataPacket input) {
    Vector3 ownGoal = Goal.ownGoal(input.car.team).center;
    Optional<BallPrediction> ballPredictionOptional = DllHelper.getBallPrediction();
    if (!ballPredictionOptional.isPresent()) {
      return input.ball.position.distance(ownGoal) < GOAL_NEARNESS_THRESHOLD;
    }

    final BallPrediction ballPrediction = ballPredictionOptional.get();

    int i = 0;
    while (i < ballPrediction.slicesLength()) {
      Physics ballPhysics = ballPrediction.slices(i).physics();
      double toGoalDistance = Vector3.of(ballPhysics.location()).distance(ownGoal);

      if (toGoalDistance < GOAL_NEARNESS_THRESHOLD) {
        return true;
      }

      i += 5;
    }

    return false;
  }

  @Override
  public Strategy.Type getType() {
    return Strategy.Type.DEFEND;
  }
}
