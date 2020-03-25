package com.eru.rlbot.bot.strats;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.common.Teams;
import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
import com.eru.rlbot.bot.path.Path;
import com.eru.rlbot.bot.plan.Marker;
import com.eru.rlbot.bot.prediction.BallPrediction;
import com.eru.rlbot.bot.prediction.BallPredictionUtil;
import com.eru.rlbot.bot.tactics.KickoffTactician;
import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;
import java.util.Optional;

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

    tacticManager.setTactic(getTactic(input));

    return true;
  }

  private int getPredictionIndex(int size) {
    return (int) (index++ % size);
  }

  private Tactic getTactic(DataPacket input) {
    // First hittable ball.
    Optional<BallPredictionUtil.ChallengeData> challengeDataOptional =
        BallPredictionUtil.get(input.car).getChallengeData();
    if (!challengeDataOptional.isPresent()) {
      // The ball cannot be hit by anyone. Grab boost and go on defense
      return getSupportPosition(input, input.ball);
    }

    BallPredictionUtil.ChallengeData challengeData = challengeDataOptional.get();
    if (challengeData.firstTouch.isHittable(input.car)) {
      // Go hit the ball
      return getStrikingTactic(input, challengeData);
    } else if (challengeData.firstTouch.isHittableByTeam(input.car.team)) {
      // Hittable by our team
      return getSupportPosition(input, challengeData.firstTouch.ball);
    }

    return getSupportPosition(input, challengeData.firstTouch.ball);
  }

  private Tactic getSupportPosition(DataPacket input, BallData firstTouchBall) {
    return Tactic.builder()
        .setTacticType(Tactic.TacticType.ROTATE)
        .setSubject(getSupportLocation(input, firstTouchBall))
        .setObject(input.ball.position)
        .build();
  }

  private Moment getSupportLocation(DataPacket input, BallData firstTouchBall) {
    Rotations rotations = Rotations.get(input);
    Goal ownGoal = Goal.ownGoal(input.car.team);

    if (rotations.isFirstMan()) {
      // Rotate to the back.
      Vector3 goalPost = Goal.ownGoal(input.car.team).getSameSidePost(input.car);
      goalPost = goalPost.addY(-Math.signum(goalPost.y) * 1000);

      return Moment.from(goalPost); // TODO: Rotate back post.
    } else if (rotations.isLastManBack()) {
      Vector3 touchGoalCenter = firstTouchBall.position.minus(ownGoal.center);
      Vector3 towardGoal = touchGoalCenter.multiply(.5);
      return Moment.from(firstTouchBall.position.plus(towardGoal));
    } else {
      CarData car = rotations.getFirstMan();
      Vector3 firstManToBall = input.ball.position.minus(car.position);
      Vector3 supportPosition = car.position.plus(firstManToBall);
      if (Math.abs(car.position.x) > 2000) {
        supportPosition = Vector3.of(Math.signum(supportPosition.x) * 500, supportPosition.y, 0);
      }
      return Moment.from(supportPosition);
    }
  }

  private Tactic getStrikingTactic(DataPacket input, BallPredictionUtil.ChallengeData challengeData) {
    Rotations rotations = Rotations.get(input);
    // TODO: Gate this on rotation priority.
    return Tactic.builder()
        .setTacticType(Tactic.TacticType.STRIKE)
        .setSubject(challengeData.firstTouch.ball)
        .setObject(getObject(challengeData.firstTouch.forCar(input.car.serialNumber)))
        .build();
  }

  private Vector3 getObject(BallPrediction.Potential potential) {
    int team = Teams.getTeamForBot(potential.index);
    Goal opponentGoal = Goal.opponentGoal(team);
    Goal ownGoal = Goal.ownGoal(team);

    if (potential.hasPlan()) {
      Path path = potential.getPath();
      CarData car = path.getTarget();
      if (path.getTarget().orientation.getNoseVector().dot(ownGoal.center) > 0) {
        double rightCorrectionAngle = Angles.flatCorrectionAngle(car, ownGoal.rightWide);
        double leftCorrectionAngle = Angles.flatCorrectionAngle(car, ownGoal.leftWide);
        return Math.abs(rightCorrectionAngle) < Math.abs(leftCorrectionAngle) ? ownGoal.rightWide : ownGoal.leftWide;
      }

      return opponentGoal.center;
    } else {
      return opponentGoal.center;
    }
  }

  @Override
  public Strategy.Type getType() {
    return Strategy.Type.ATTACK;
  }
}
