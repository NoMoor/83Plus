package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.BoostPathHelper;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
import com.eru.rlbot.bot.maneuver.WaveDash;
import com.eru.rlbot.bot.prediction.BallPrediction;
import com.eru.rlbot.bot.prediction.BallPredictionUtil;
import com.eru.rlbot.bot.prediction.CarLocationPredictor;
import com.eru.rlbot.bot.prediction.CarPrediction;
import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.boost.BoostPad;
import com.eru.rlbot.common.input.BoundingBox;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.Controls;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.Optional;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manages tactical demos. In truth, this is a ball chasing, demo-ing machine.
 */
public class DemoTactician extends Tactician {

  private static final Logger logger = LogManager.getLogger("DemoTactician");

  private boolean completed;

  DemoTactician(ApolloGuidanceComputer bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  @Override
  public void internalExecute(DataPacket input, Controls output, Tactic tactic) {
    Optional<Vector3> optionalTarget = getDashTarget(input);

    if (!optionalTarget.isPresent()) {
      return;
    }

    Vector3 target = optionalTarget.get();

    Vector3 carTarget = target.minus(input.car.position);
    boolean travelingTowardTarget = carTarget.dot(input.car.velocity) >= 0;
    boolean pointingAtTarget = carTarget.dot(input.car.orientation.getNoseVector()) >= 0;
    double targetCorrectionAngle = Angles.flatCorrectionAngle(input.car, target);

    double timeToTarget = carTarget.magnitude() / input.car.groundSpeed;
    double velocityTargetCorrection = Angles.flatCorrectionAngle(input.car.position, input.car.velocity, target);
    double quarterVelocityTargetCorrection = Math.abs(velocityTargetCorrection);
    if (quarterVelocityTargetCorrection > Math.PI / 2) {
      quarterVelocityTargetCorrection = Math.PI - quarterVelocityTargetCorrection;
    }

    if (input.car.hasWheelContact) {
      if (!travelingTowardTarget && Math.abs(velocityTargetCorrection) > .3) {
        bot.botRenderer.setBranchInfo("Going the wrong way %f", velocityTargetCorrection);
        output
            .withSteer(targetCorrectionAngle * 10)
            .withThrottle(1.0)
            .withSlide(!pointingAtTarget && Math.abs(targetCorrectionAngle) > Math.PI / 3);
      } else if (input.car.isSupersonic || input.car.groundSpeed > 1900 || timeToTarget < WaveDash.MIN_DASH_TIME) {
        bot.botRenderer.setBranchInfo("Aim at target");

        output
            .withThrottle(1.0)
            .withBoost(input.car.groundSpeed < 2210)
            .withSteer(Angles.flatCorrectionAngle(input.car, target) * 10);
      } else if (input.car.groundSpeed < 1000) {
        bot.botRenderer.setBranchInfo("Speed up");
        output
            .withThrottle(1.0f)
            .withBoost(input.car.boost > 30)
            .withSteer(Angles.flatCorrectionAngle(input.car, target) * 10);
      } else if (Math.abs(targetCorrectionAngle) > .3 && Math.abs(quarterVelocityTargetCorrection) > .8) {
        bot.botRenderer.setBranchInfo("Correct before jump");
        double velocityCorrection = Angles.flatCorrectionAngle(input.car, input.car.velocity);
        output
            .withThrottle(1.0f)
            .withBoost(input.car.boost > 30)
            .withSteer(Angles.flatCorrectionAngle(input.car, target) * 5)
            .withSlide(Math.abs(velocityCorrection) > .4);
      } else if (input.car.angularVelocity.flatten().magnitude() < .5) {
        delegateToWaveDash(input, output, target);
      } else {
        output
            .withSlide()
            .withThrottle(1.0);
      }
    } else {
      delegateToWaveDash(input, output, target);
    }
  }

  private Optional<Vector3> getDashTarget(DataPacket input) {
    CarPrediction.PredictionNode demoTarget = null;
    if (input.allCars.size() > 1) {
      Optional<CarPrediction.PredictionNode> demoTargetOptional = input.allCars.stream()
          .filter(car -> car.team != input.car.team)
          .filter(car -> !car.isDemolished)
          .map(earliestTarget(input.car))
          .filter(node -> Angles.isInFrontOfCar(input.car, node.position))
          .findFirst();

      if (demoTargetOptional.isPresent()) {
        demoTarget = demoTargetOptional.get();

        bot.botRenderer.setBranchInfo("Demo target acquired");
      }
    }

    BallPrediction ball = BallPredictionUtil.get(input.car).getFirstHittableLocation();

    Vector3 target;
    if (demoTarget != null && ball != null) {
      // TODO: Check if the ball is a break-away.
      target = ball.ball.time < demoTarget.getAbsoluteTime() ? ball.ball.position : demoTarget.position;
    } else if (demoTarget != null) {
      target = demoTarget.position;
    } else if (ball != null) {
      target = ball.ball.position;
    } else {
      target = Goal.ownGoal(input.car.team).getSameSidePost(input.car);
    }

    if (input.car.boost < 5 && !input.car.isSupersonic) {
      // Bail out and go for boost.
      pickupBoost(input, target);
      return Optional.empty();
    }

    Optional<BoostPad> nearestPad = BoostPathHelper.boostOnTheWay(input.car, target);
    if (nearestPad.isPresent() && input.car.boost < 80) {
      bot.botRenderer.setBranchInfo("Boost target acquired");
      target = BoostPathHelper.getNearestBoostEdge(input.car.position, target, nearestPad.get());
    }

    return Optional.of(target);
  }

  private void delegateToWaveDash(DataPacket input, Controls output, Vector3 target) {
    double correctionAngle = Angles.flatCorrectionAngle(input.car, target);
    // Do Initial jump.
    output
        .withThrottle(1.0)
        .withJump(input.car.velocity.z < 40 && input.car.hasWheelContact)
        .withSlide(Math.abs(correctionAngle) > .2);

    bot.botRenderer.setBranchInfo("Delegate");
    delegateTo(WaveDash.builder()
        .withTarget(target)
        .withBoost(6)
        .build());
  }

  private void pickupBoost(DataPacket input, Vector3 secondaryTarget) {
    bot.botRenderer.setBranchInfo("Need boost");
    completed = true;

    Optional<BoostPad> nearestPad = BoostPathHelper.nearestBoostPad(input.car);
    Moment subject = nearestPad.map(Moment::from)
        .orElseGet(() -> Moment.from(Goal.ownGoal(input.alliance).center));
    tacticManager.setTactic(Tactic.builder()
        .setSubject(subject)
        .setObject(secondaryTarget)
        .setTacticType(Tactic.TacticType.ROTATE)
        .build());
  }

  private Function<CarData, CarPrediction.PredictionNode> earliestTarget(CarData self) {
    CarLocationPredictor locationPredictor = CarLocationPredictor.forCar(self);
    double increasedSpeed = Constants.SUPER_SONIC - self.groundSpeed;
    double timeToSuperSonic = increasedSpeed < 0 ? 0 : increasedSpeed / Constants.BOOSTED_MAX_SPEED;
    double distanceToSuperSonic = timeToSuperSonic * ((self.groundSpeed + Constants.SUPER_SONIC) / 2);
    return car -> {
      ImmutableList<CarPrediction.PredictionNode> predictions = locationPredictor.forOpponent(car).getPredictions();
      return predictions.stream()
          .filter(slice -> {
            // Subtract the length of the front of the car so we don't accidentally t-bone ourself on an opponent going
            // super sonic.
            double distance = slice.position.distance(self.position) - BoundingBox.frontToRj;
            double superSonicDistance = distance - distanceToSuperSonic;
            double superSonicTime = superSonicDistance / Constants.SUPER_SONIC;
            double timeToSlice = slice.absoluteTime - self.elapsedSeconds;
            return superSonicTime > 0 && superSonicTime + timeToSuperSonic < timeToSlice;
          })
          .findFirst()
          .orElse(Iterables.getLast(predictions));
    };
  }

  @Override
  public boolean isLocked() {
    return !completed;
  }
}
