package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.common.*;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.flat.PredictionSlice;
import java.util.Optional;

public class ShadowTactician extends Tactician {

  ShadowTactician(EruBot bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  @Override
  public void execute(DataPacket input, ControlsOutput output, Tactic tactic) {
    double minCorrection = Locations.minCarTargetNotGoalCorrection(input, tactic.subject);
    double targetCorrectionAngle = Angles.flatCorrectionAngle(input.car, tactic.subject.position);

    bot.botRenderer.setBranchInfo("Get along side");
    if (Math.abs(minCorrection) > 0) {
      getAlongSide(input, output, tactic);
    } else {
      bot.botRenderer.setBranchInfo("Sweep");
      // TODO: Make this better
      float ballToTargetTime = tactic.subject.time - input.car.elapsedSeconds;
      float carToTargetTime = Accels.timeToDistance(input.car.velocity.norm(), input.car.position.distance(tactic.subject.position));

      output
          .withSteer(targetCorrectionAngle * 5)
          .withThrottle(ballToTargetTime > carToTargetTime + .1 ? -1 : ballToTargetTime > carToTargetTime ? 0 : 1.0)
          .withBoost(ballToTargetTime < carToTargetTime + .1);
    }
  }

  private void getAlongSide(DataPacket input, ControlsOutput output, Tactic tactic) {
    float ballToGoalTime = getBallToGoalTime(input);
    float carToGoalTime = getCarToGoalTime(input);

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

  private float getCarToGoalTime(DataPacket input) {
    Optional<PredictionSlice> slice = PredictionUtils.getBallInGoalSlice();
    if (!slice.isPresent()) {
      return 0;
    }

    Vector3 goalLocation = Vector3.of(slice.get().physics().location());
    double distanceToSave = input.car.position.distance(goalLocation);

    return Accels.timeToDistance(input.car.velocity.flatten().norm(), distanceToSave);
  }

  private float getBallToGoalTime(DataPacket input) {
    Optional<PredictionSlice> slice = PredictionUtils.getBallInGoalSlice();
    if (!slice.isPresent()) {
      return 0;
    }

    return slice.get().gameSeconds() - input.car.elapsedSeconds;
  }
}
