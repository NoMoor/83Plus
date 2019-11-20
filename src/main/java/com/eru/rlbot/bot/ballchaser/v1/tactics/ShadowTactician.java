package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.common.*;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.flat.BallPrediction;
import rlbot.flat.PredictionSlice;
import java.util.Optional;

public class ShadowTactician extends Tactician {

  ShadowTactician(EruBot bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  @Override
  public void execute(DataPacket input, ControlsOutput output, Tactic tactic) {
    double minCorrection = Locations.minCarTargetNotGoalCorrection(input, tactic.targetMoment);
    double targetCorrectionAngle = Angles.flatCorrectionDirection(input.car, tactic.targetMoment.position);

    if (Math.abs(minCorrection) > getSpeedAdjustedMinAngle(input.car, targetCorrectionAngle)) {
      bot.botRenderer.setBranchInfo("Get along side %f", minCorrection);
      getAlongSide(input, output, tactic);
    } else {

      bot.botRenderer.setBranchInfo("Sweep %f", targetCorrectionAngle);
      // TODO: Make this better
      float ballToTargetTime = tactic.targetMoment.time - input.car.elapsedSeconds;
      float carToTargetTime = Accels.timeToDistance(input.car.velocity.norm(), input.car.position.distance(tactic.targetMoment.position));

      output
          .withSteer(targetCorrectionAngle)
          .withThrottle(ballToTargetTime > carToTargetTime ? -1 : 1.0);
    }
  }

  private final double MIN_ANGLE_GAIN = 0.0003f;
  private double getSpeedAdjustedMinAngle(CarData car, double targetCorrectionAngle) {
    double groundspeed = car.groundSpeed;

    // Faster + bigger angle = larger number.
    // 2000 * .5 * x = .3
    return groundspeed * Math.abs(targetCorrectionAngle) * MIN_ANGLE_GAIN;
//    return .15f;
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

      double correctionAngle = Angles.flatCorrectionDirection(input.car, tactic.targetMoment.position);
      if (Math.abs(correctionAngle) < .5) {
        output.withSteer(Locations.minCarTargetNotGoalCorrection(input, tactic.targetMoment));
      }
    }
  }

  private float getCarToGoalTime(DataPacket input) {
    PredictionSlice slice = getBallInGoalSlice();
    if (slice == null) {
      return 0;
    }

    Vector3 goalLocation = Vector3.of(slice.physics().location());
    double distanceToSave = input.car.position.distance(goalLocation);

    return Accels.timeToDistance(input.car.velocity.flatten().norm(), distanceToSave);
  }

  private float getBallToGoalTime(DataPacket input) {
    PredictionSlice slice = getBallInGoalSlice();
    if (slice == null) {
      return 0;
    }

    return slice.gameSeconds() - input.car.elapsedSeconds;
  }

  private PredictionSlice getBallInGoalSlice() {
    Optional<BallPrediction> ballPredictionOptional = DllHelper.getBallPrediction();
    if (!ballPredictionOptional.isPresent()) {
      return null;
    }

    BallPrediction ballPrediction = ballPredictionOptional.get();
    for (int i = 0 ; i < ballPrediction.slicesLength() ; i++) {
      PredictionSlice predictionSlice = ballPrediction.slices(i);
      if (Math.abs(predictionSlice.physics().location().y()) > Constants.HALF_LENGTH + Constants.BALL_RADIUS) {
        return predictionSlice;
      }
    }

    return null;
  }
}
