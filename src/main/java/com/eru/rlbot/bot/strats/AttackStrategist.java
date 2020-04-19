package com.eru.rlbot.bot.strats;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.CarBall;
import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.common.SupportRegions;
import com.eru.rlbot.bot.common.Teams;
import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
import com.eru.rlbot.bot.path.Path;
import com.eru.rlbot.bot.prediction.BallPrediction;
import com.eru.rlbot.bot.prediction.BallPredictionUtil;
import com.eru.rlbot.bot.prediction.CarLocationPredictor;
import com.eru.rlbot.bot.tactics.KickoffTactician;
import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;
import java.awt.Color;
import java.util.Comparator;
import java.util.Optional;

/**
 * Responsible for dribbling, shooting, and passing.
 */
public class AttackStrategist extends Strategist {

  AttackStrategist(ApolloGuidanceComputer bot) {
    super(bot);
  }

  @Override
  public boolean assign(DataPacket input) {
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

    tacticManager.setTactic(getTactic(input));

    return true;
  }

  private Tactic getTactic(DataPacket input) {

    // First hittable ball.
    Optional<BallPredictionUtil.ChallengeData> challengeDataOptional =
        BallPredictionUtil.get(input.car).getChallengeData();
    if (!challengeDataOptional.isPresent()) {
      bot.botRenderer.addAlertText("Cannot get challenge data.");
      // The ball cannot be hit by anyone. Grab boost and go on defense
      return Teams.getTeamSize(input.car.team) > 1
          ? getStrikingTactic(input)
          : getSupportTactic(input, input.ball);
    }

    Rotations rotations = Rotations.get(input);
    BallPredictionUtil.ChallengeData challengeData = challengeDataOptional.get();
    if (challengeData.firstTouch.isHittable(input.car)
        || (rotations.isLastManBack() && rotations.hasPriority())) {
      // Go hit the ball
      return getStrikingTactic(input, challengeData);
    } else if (challengeData.firstTouch.isHittableByTeam(input.car.team)) {
      return getStrikingTactic(input, challengeData);
    }

    return getStrikingTactic(input, challengeData);
  }

  private Tactic getSupportTactic(DataPacket input, BallData firstTouchBall) {
    return Tactic.builder()
        .setTacticType(Tactic.TacticType.ROTATE)
        .setSubject(getSupportLocation(input, firstTouchBall))
        .setObject(input.ball.position)
        .build();
  }

  private Moment getSupportLocation(DataPacket input, BallData firstTouchBall) {
    Goal ownGoal = Goal.ownGoal(input.car.team);

    Vector3 mostDefendingAlly = CarLocationPredictor.forCar(input.car).teammates().stream()
        .map(CarLocationPredictor.CarLocationPrediction::oneSec)
        .min(Comparator.comparing(predictedLocation -> predictedLocation.distance(ownGoal.center)))
        .orElse(input.ball.position);

    bot.botRenderer.renderTarget(Color.PINK, mostDefendingAlly);
    return Moment.from(SupportRegions.getSupportRegions(mostDefendingAlly, input.car.team));
  }

  private Tactic getStrikingTactic(DataPacket input) {
    BallPrediction target = BallPredictionUtil.get(input.car).getTarget();
    BallData subject = target.ball;
    Vector3 object = Goal.opponentGoal(input.car.team).center;
    Tactic.TacticType type = target.getTacticType();

    return Tactic.builder()
        .setTacticType(type)
        .setSubject(subject)
        .setObject(object)
        .build();
  }

  private Tactic getStrikingTactic(DataPacket input, BallPredictionUtil.ChallengeData challengeData) {
    if (challengeData.controllingTeam == input.car.team) {
      BallPrediction firstTouch = BallPredictionUtil.get(input.car).getTarget();
      BallPrediction.Potential potential = firstTouch.forCar(input.car.serialNumber);

      if (potential == null || !potential.hasPlan()) {
        return getStrikingTactic(input);
      }

      BallData subject = firstTouch.ball;
      Vector3 object = getObject(potential);
      Tactic.TacticType type = potential.getPlan().type;

      return Tactic.builder()
          .setTacticType(type)
          .setSubject(subject)
          .setObject(object)
          .build();
    } else {
      return getStrikingTactic(input);
    }
  }

  private Vector3 getObject(BallPrediction.Potential potential) {
    int team = Teams.getTeamForBot(potential.index);
    Goal opponentGoal = Goal.opponentGoal(team);
    Goal ownGoal = Goal.ownGoal(team);

    if (potential.hasPlan()) {
      Path path = potential.getPath();

      if (path != null) {
        CarData car = path.getTarget();
        Vector3 carToGoal = ownGoal.center.minus(car.position);
        if (path.getTarget().orientation.getNoseVector().dot(carToGoal) > 0) {
          double rightCorrectionAngle = Angles.flatCorrectionAngle(car, ownGoal.rightWide);
          double leftCorrectionAngle = Angles.flatCorrectionAngle(car, ownGoal.leftWide);
          return Math.abs(rightCorrectionAngle) < Math.abs(leftCorrectionAngle) ? ownGoal.rightWide : ownGoal.leftWide;
        }
      }

      return opponentGoal.centerTop;
    } else {
      return opponentGoal.centerTop;
    }
  }

  @Override
  public Strategy.Type getType() {
    return Strategy.Type.ATTACK;
  }

  @Override
  public Strategy.Type getDelegate() {
    return Strategy.Type.ROTATE;
  }

  @Override
  public boolean isComplete(DataPacket input) {
    Rotations rotations = Rotations.get(input);

    return !CarBall.ballIsUpfield(input) || rotations.isLastManBack();
  }
}
