package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.common.*;
import com.eru.rlbot.common.Moment;
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
    Moment target = getFirstHittableLocation(input);

    // Adjust location to push it away from the center of the goal.
    target = adjustTarget(target);

    bot.botRenderer.setCarTarget(target.position);

    // Stay between the ball and the goal.
    double correctionAngle = Angles.flatCorrectionAngle(input.car, target.position);

    double carTimeToTarget = Accels.timeToDistance(input.car.groundSpeed, input.car.position.distance(target.position));
    double ballTimeToTarget = target.time - input.car.elapsedSeconds;

    output
        .withSteer(correctionAngle)
        .withThrottle(carTimeToTarget < ballTimeToTarget ? 0 : 1f);

    Optional<Float> zTimeToTarget = Accels.jumpTimeToHeight(target.position.z - Constants.BALL_RADIUS);

    if (zTimeToTarget.isPresent() && zTimeToTarget.get() > carTimeToTarget) {
//      bot.botRenderer.setBranchInfo("Time to Z %f", zTimeToTarget.get());
      // TODO: Adjust this jump better.
      output.withJump();
    } else if (Math.abs(correctionAngle) > 1.5) {
      output.withSlide();
      sliding = true;
    } else if (sliding && Math.abs(correctionAngle) > 1.2) {
      output.withSlide()
          .withBoost();
    } else {
      sliding = false;
    }
  }

  private Moment adjustTarget(Moment target) {
    Vector3 adjustedTarget = target.position.addX(-Math.signum(target.position.x) * Constants.BALL_RADIUS / 2);
    return new Moment(adjustedTarget, target.velocity, target.time);
  }

  private Moment getFirstHittableLocation(DataPacket input) {
    Optional<BallPrediction> ballPredictionOptional = DllHelper.getBallPrediction();
    if (ballPredictionOptional.isPresent()) {
      BallPrediction ballPrediction = ballPredictionOptional.get();
      for (int i = 0 ; i < ballPrediction.slicesLength() ; i++) {
        PredictionSlice predictionSlice = ballPrediction.slices(i);
        Vector3 ballLocation = Vector3.of(predictionSlice.physics().location());
        double ballDistance = ballLocation.minus(input.car.position).norm() - Constants.BALL_RADIUS;

        double timeToDistance = Accels.timeToDistance(input.car.velocity.flatten().norm(), ballDistance);
        if (input.car.elapsedSeconds + timeToDistance < predictionSlice.gameSeconds() && ballLocation.z < 300) {
          return new Moment(predictionSlice);
        }
      }
    }

    return new Moment(input.ball, input.car.elapsedSeconds);
  }
}
