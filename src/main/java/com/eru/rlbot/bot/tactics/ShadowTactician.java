package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.common.Accels;
import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Locations;
import com.eru.rlbot.bot.common.PredictionUtils;
import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.Controls;
import com.eru.rlbot.common.vector.Vector3;
import java.util.Optional;
import rlbot.flat.PredictionSlice;

/**
 * Aspirational at best.
 */
public class ShadowTactician extends Tactician {

  ShadowTactician(ApolloGuidanceComputer bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  @Override
  public void internalExecute(DataPacket input, Controls output, Tactic tactic) {
    double minCorrection = Locations.minCarTargetNotGoalCorrection(input, tactic.subject);
    double targetCorrectionAngle = Angles.flatCorrectionAngle(input.car, tactic.subject.position);

    bot.botRenderer.setBranchInfo("Get along side");
    if (Math.abs(minCorrection) > 0) {
      getAlongSide(input, output, tactic);
    } else {
      bot.botRenderer.setBranchInfo("Sweep");
      // TODO: Make this better
      float ballToTargetTime = tactic.subject.time - input.car.elapsedSeconds;
      double carToTargetTime = Accels.nonBoostedTimeToDistance(
          input.car.velocity.magnitude(),
          input.car.position.distance(tactic.subject.position)).getDuration();

      output
          .withSteer(targetCorrectionAngle * 5)
          .withThrottle(ballToTargetTime > carToTargetTime + .1 ? -1 : ballToTargetTime > carToTargetTime ? 0 : 1.0)
          .withBoost(ballToTargetTime < carToTargetTime + .1);
    }
  }

  private void getAlongSide(DataPacket input, Controls output, Tactic tactic) {
    float ballToGoalTime = getBallToGoalTime(input);
    double carToGoalTime = getCarToGoalTime(input);

    if (ballToGoalTime < carToGoalTime + 1) {
      output
          .withThrottle(1.0f)
          .withBoost(ballToGoalTime < carToGoalTime + 1);
    } else {
      output.withThrottle(.02f);

      double correctionAngle = Angles.flatCorrectionAngle(input.car, tactic.subject.position);
      if (Math.abs(correctionAngle) < .5) {
        output.withSteer(Locations.minCarTargetNotGoalCorrection(input, tactic.subject));
      }
    }
  }

  private double getCarToGoalTime(DataPacket input) {
    Optional<PredictionSlice> slice = PredictionUtils.getBallInGoalSlice();
    if (!slice.isPresent()) {
      return 0;
    }

    Vector3 goalLocation = Vector3.of(slice.get().physics().location());
    double distanceToSave = input.car.position.distance(goalLocation);

    return Accels.nonBoostedTimeToDistance(input.car.velocity.flatten().magnitude(), distanceToSave).getDuration();
  }

  private float getBallToGoalTime(DataPacket input) {
    Optional<PredictionSlice> slice = PredictionUtils.getBallInGoalSlice();
    if (!slice.isPresent()) {
      return 0;
    }

    return slice.get().gameSeconds() - input.car.elapsedSeconds;
  }
}
