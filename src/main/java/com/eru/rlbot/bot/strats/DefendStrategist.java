package com.eru.rlbot.bot.strats;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.CarBall;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.common.Locations;
import com.eru.rlbot.bot.common.SupportRegions;
import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
import com.eru.rlbot.bot.plan.Marker;
import com.eru.rlbot.bot.prediction.BallPredictionUtil;
import com.eru.rlbot.bot.tactics.KickoffTactician;
import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.common.DllHelper;
import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;
import java.util.Optional;
import rlbot.flat.BallPrediction;
import rlbot.flat.Physics;

/** Responsible for shadowing, blocking, shots, and clearing. */
public class DefendStrategist extends Strategist {

  DefendStrategist(ApolloGuidanceComputer bot) {
    super(bot);
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

  @Override
  public boolean assign(DataPacket input) {
    Marker.get(input.serialNumber).markNext(input);

    if (tacticManager.isTacticLocked()) {
      // Let the tactic finish it's motion.
      bot.botRenderer.addAlertText("Keep tactic");
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

    com.eru.rlbot.bot.prediction.BallPrediction firstHittableTarget = BallPredictionUtil.get(input.car).getTarget();
    BallData subject = firstHittableTarget != null ? firstHittableTarget.ball : input.ball;

    // TODO: Update to include the opponent hitting the ball
    if (shotOnGoal(input)) {
      tacticManager.setTactic(Tactic.builder()
          .setSubject(subject)
          .setObject(getObject(input, firstHittableTarget))
          .setTacticType(getTacticType(firstHittableTarget))
          .build());
    } else if (canClear(input) && Locations.carToBall(input).magnitude() > 2000) {
      tacticManager.setTactic(Tactic.builder()
          .setSubject(subject)
          .setObject(getObject(input, firstHittableTarget))
          .setTacticType(Tactic.TacticType.ROTATE)
          .build());
    } else if (canClear(input)) {
      tacticManager.setTactic(Tactic.builder()
          .setSubject(subject)
          .setObject(getObject(input, firstHittableTarget))
          .setTacticType(getTacticType(firstHittableTarget))
          .build());
    } else {
      Vector3 ownGoal = Goal.ownGoal(input.car.team).center;
      ownGoal = ownGoal.addY(1500 * -Math.signum(ownGoal.y));

      SupportRegions regions = SupportRegions.getSupportRegions(ownGoal, input.car.team);
      tacticManager.setTactic(Tactic.builder()
          .setSubject(Moment.from(regions))
          .setTacticType(Tactic.TacticType.GUARD)
          .build());
    }

    return true;
  }

  private Tactic.TacticType getTacticType(com.eru.rlbot.bot.prediction.BallPrediction firstHittableTarget) {
    return firstHittableTarget != null ? firstHittableTarget.getTacticType() : Tactic.TacticType.AERIAL;
  }

  private Vector3 getObject(DataPacket input, com.eru.rlbot.bot.prediction.BallPrediction firstHittableTarget) {
    if (firstHittableTarget == null) {
      return input.ball.position;
    }

    BallData firstHittableBall = firstHittableTarget.ball;
    return firstHittableBall.position.plus(firstHittableBall.position.minus(input.car.position).toMagnitude(2000));
  }

  private static boolean canClear(DataPacket input) {
    boolean ballIsInFrontOfCar = Math.abs(Angles.flatCorrectionAngle(input.car, input.ball.position)) < .75;
    boolean ballNearGoal = ballNearGoal(input);
    boolean isPointedAwayFromGoal =
        Math.abs(Locations.minCarTargetNotGoalCorrection(input, Moment.from(input.ball))) < .2;

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

  @Override
  public Strategy.Type getDelegate() {
    return Strategy.Type.ATTACK;
  }

  @Override
  public boolean isComplete(DataPacket input) {
    Rotations rotations = Rotations.get(input);
    return !rotations.isLastManBack() && CarBall.ballIsUpfield(input);
  }
}
