package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.common.*;
import com.eru.rlbot.common.input.BallData;
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
  public void execute(DataPacket input, ControlsOutput output, Tactic nextTactic) {
    BallData noseRelativeBall = NormalUtils.noseRelativeBall(input);

    float positionVelocityRatio = noseRelativeBall.position.y / noseRelativeBall.velocity.y;

    if (Math.abs(positionVelocityRatio) > .15) {
      getAlongSide(input, output);
    } else {
      // TODO: Make this better
      output
          .withSteer(-Math.signum(noseRelativeBall.position.x));
    }
  }

  private void getAlongSide(DataPacket input, ControlsOutput output) {
    float ballToGoalTime = getBallToGoalTime(input);
    float carToGoalTime = getCarToGoalTime(input);

    if (ballToGoalTime < carToGoalTime + 1) {
      output
          .withThrottle(1.0f)
          .withBoost(ballToGoalTime < carToGoalTime + 2);
    } else {
      output.withThrottle(.02f);
    }

    output.withSteer(Angles.flatCorrectionDirection(input.car, input.ball.position.addX(135)));
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
