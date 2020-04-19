package com.eru.rlbot.bot.strats;

import com.eru.rlbot.bot.common.CarBall;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.common.SupportRegions;
import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
import com.eru.rlbot.bot.prediction.BallPredictionUtil;
import com.eru.rlbot.bot.prediction.BallPredictor;
import com.eru.rlbot.bot.prediction.CarBallCollision;
import com.eru.rlbot.bot.tactics.KickoffTactician;
import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.common.DllHelper;
import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;
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

    BallPredictionUtil ballPredictionUtil = BallPredictionUtil.get(input.car);
    Optional<BallPredictionUtil.ChallengeData> challengeDataOptional = ballPredictionUtil.getChallengeData();

    com.eru.rlbot.bot.prediction.BallPrediction firstHittableTarget = BallPredictionUtil.get(input.car).getTarget();
    BallData subject = firstHittableTarget != null ? firstHittableTarget.ball : input.ball;
    if (challengeDataOptional.isPresent()) {
      BallPredictionUtil.ChallengeData challengeData = challengeDataOptional.get();
      if (challengeData.controllingTeam != input.car.team) {
        // Simulate the possiblity of a hit.
        BallData firstTouchBall = challengeData.firstTouch.ball;
        CarData strikingCar = challengeData.firstTouch.forCar(challengeData.firstTouch.ableToReach().get(0))
            .getPath().getTarget();
        BallData resultingBall = CarBallCollision.calculateCollision(firstTouchBall, strikingCar);
        ImmutableList<BallData> predictions = BallPredictor.makePrediction(resultingBall);
        bot.botRenderer.renderBallPrediction(predictions);

        double correctionTime;
        if (firstHittableTarget != null) {
          double timeOffset = firstHittableTarget.ball.time - challengeData.firstTouch.ball.time;
          correctionTime = challengeData.firstTouch.ball.time + (timeOffset * .08);
        } else {
          double timeToContact = challengeData.firstTouch.ball.time - input.car.elapsedSeconds;
          correctionTime = challengeData.firstTouch.ball.time + (timeToContact * .08);
        }

        Optional<BallData> projectedBallPositionAtCollisionTime = predictions.stream()
            .filter(ball -> ball.time > correctionTime)
            .findFirst();

        if (projectedBallPositionAtCollisionTime.isPresent()) {
          subject = projectedBallPositionAtCollisionTime.get();
        }
      }
    }

    if (shotOnGoal(input)) {
      tacticManager.setTactic(Tactic.builder()
          .setSubject(subject)
          .setObject(getObject(input, firstHittableTarget))
          .setTacticType(getTacticType(firstHittableTarget))
          .build());
    } else if (canClear(input)) {
      tacticManager.setTactic(Tactic.builder()
          .setSubject(subject)
          .setObject(getObject(input, firstHittableTarget))
          .setTacticType(getTacticType(firstHittableTarget))
          .build());
    } else {
      SupportRegions regions = SupportRegions.getSupportRegions(input.ball.position, input.car.team);
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
    Rotations rotations = Rotations.get(input);
    CarData secondManBack = rotations.getNextToLastManBack();

    Vector3 nearPost = Goal.ownGoal(input.car.team).getNearPost(input.ball.position);
    double secondManDistance = secondManBack.position.distance(nearPost);
    double ballToGoal = input.ball.position.distance(nearPost);

    // TODO: Improve this.
    if (rotations.isLastManBack() && (secondManDistance < ballToGoal)) {
      return true;
    }
    return ballToGoal < 5000 || input.ball.position.distance(input.car.position) < 3000;
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
