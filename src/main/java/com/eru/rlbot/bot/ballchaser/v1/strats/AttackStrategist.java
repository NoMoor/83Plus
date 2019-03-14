package com.eru.rlbot.bot.ballchaser.v1.strats;

import static com.eru.rlbot.bot.common.Constants.ACCELERATION;

import com.eru.rlbot.bot.ballchaser.v1.tactics.Tactic;
import com.eru.rlbot.bot.ballchaser.v1.tactics.TacticManager;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.Bot;
import rlbot.cppinterop.RLBotDll;
import rlbot.flat.BallPrediction;
import rlbot.flat.PredictionSlice;
import java.io.IOException;


/** Responsible for dribbling, shooting, and passing. */
public class AttackStrategist implements Strategist {

  private final TacticManager tacticManager;

  public AttackStrategist(Bot bot) {
    this.tacticManager = new TacticManager(bot);
  }

  // TODO: This method should actually do path finding for the 'goal'.
  @Override
  public boolean assign(DataPacket input) {
    // TODO: This should probably just adjust the spot that the ball is so any boosts or w/e can stay.
    try {
      double maxScore = Double.MAX_VALUE;

      BallPrediction ballPrediction = RLBotDll.getBallPrediction();

      if (ballPrediction.slicesLength() == 0) {
        return false;
      }

      PredictionSlice bestTarget = ballPrediction.slices(ballPrediction.slicesLength());
      for (int i = 0 ; i < ballPrediction.slicesLength() ; i++) {
        PredictionSlice predictionSlice = ballPrediction.slices(i);
        double score = createBallScore(input, predictionSlice);
        if (score > 0 && score < maxScore) {
          maxScore = score;
          bestTarget = predictionSlice;
        }
      }

      tacticManager.setTactic(new Tactic(bestTarget, Tactic.Type.HIT_BALL));

      return true;
    } catch (Exception e) {
      e.printStackTrace();
    }

    return false;
  }

  @Override
  public boolean isComplete(DataPacket input) {
    return false;
  }

  @Override
  public void abort() {
    tacticManager.clearTactics();
  }

  @Override
  public ControlsOutput execute(DataPacket input) {
    ControlsOutput output = new ControlsOutput();
    tacticManager.execute(input, output);
    return output;
  }

  private static double createBallScore(DataPacket input, PredictionSlice predictionSlice) {
    Vector3 location = new Vector3(predictionSlice.physics().location());

    double timeToPosition = timeToPosition(input.car, location);

    double ballTimeToPosition = (predictionSlice.gameSeconds() - input.car.elapsedSeconds);

    // If we cannot get there in time to hit it.
    if (timeToPosition > ballTimeToPosition) {
      // Can't get there in time.
      return Double.MAX_VALUE;
    }

    return predictionSlice.gameSeconds();
  }

  private static double timeToPosition(CarData carData, Vector3 target) {
    double distanceToPosition = carData.position.distance(target);

    // Am I heading the correct direction?
    Vector3 futurePosition = carData.position.plus(carData.velocity.scaled(1.0 / 60)); // is this v per second?
    boolean correctDirection = futurePosition.distance(target) < distanceToPosition;

    if (correctDirection) {
      // TODO(ahatfield): Change this to be only the velocity in the correct direction.
      return distanceToPosition / (carData.velocity.flatten().magnitude() + (ACCELERATION / 2));
    } else {
      // time to turn around + time to ball
      // TODO(ahatfield): Plus some distance to turn around.
      return (carData.velocity.magnitude() / ACCELERATION)
                 + distanceToPosition / (carData.velocity.flatten().magnitude() + (ACCELERATION / 2));
    }
  }

  @Override
  public Strategy.Type getType() {
    return Strategy.Type.ATTACK;
  }
}
