package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.common.Accels;
import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.common.DllHelper;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.flat.BallPrediction;
import rlbot.flat.PredictionSlice;
import java.util.Optional;

public class GoalLineTactician extends Tactician {

  private boolean sliding;

  GoalLineTactician(EruBot bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  @Override
  public void execute(DataPacket input, ControlsOutput output, Tactic tactic) {
    Vector3 target = getFirstHittableLocation(input);

    bot.botRenderer.setCarTarget(target);

    // Stay between the ball and the goal.
    double correctionAngle = Angles.flatCorrectionDirection(input.car, target);

    output
        .withSteer(correctionAngle)
        .withThrottle(1.0f);

    if (Math.abs(correctionAngle) > 1.5) {
      output.withSlide();
      sliding = true;
    } else if (sliding && Math.abs(correctionAngle) > 1.0) {
      output.withSlide()
          .withBoost();
    } else {
      sliding = false;
    }
  }

  private Vector3 getFirstHittableLocation(DataPacket input) {
    Optional<BallPrediction> ballPredictionOptional = DllHelper.getBallPrediction();
    if (!ballPredictionOptional.isPresent()) {
      return input.ball.position;
    }

    BallPrediction ballPrediction = ballPredictionOptional.get();
    for (int i = 0 ; i < ballPrediction.slicesLength() ; i++) {
      PredictionSlice predictionSlice = ballPrediction.slices(i);
      Vector3 ballLocation = Vector3.of(predictionSlice.physics().location());
      double ballDistance = ballLocation.minus(input.car.position).norm() - Constants.BALL_RADIUS;

      double timeToDistance = Accels.timeToDistance(input.car.velocity.flatten().norm(), ballDistance);
      if (input.car.elapsedSeconds + timeToDistance < predictionSlice.gameSeconds()) {
        return ballLocation;
      }
    }

    return input.ball.position;
  }
}
