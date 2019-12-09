package com.eru.rlbot.bot.tactics;

import static com.eru.rlbot.bot.common.Constants.HALF_LENGTH;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.DllHelper;
import com.eru.rlbot.bot.main.Agc;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.flat.BallPrediction;
import rlbot.flat.PredictionSlice;
import java.util.Optional;

public class RollingTactician extends Tactician {

  private boolean amBoosting;

  RollingTactician(Agc bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  @Override
  public void execute(DataPacket input, ControlsOutput output, Tactic tactic) {
    // TODO: Fix this

    Vector2 carDirection = input.car.orientation.getNoseVector().flatten();
    Vector3 targetPosition = tactic.subject.position;

    // Subtract the two positions to get a vector pointing from the car to the ball.
    Vector3 carToTarget = targetPosition.minus(input.car.position);

    double flatCorrectionAngle;
    // Need to go up the wall.
    if (input.car.hasWheelContact
            && Math.abs(carToTarget.z) > 500 // Needs to go upward
            && Math.abs(input.car.position.x) > 3000 // Near the wall
            && input.car.position.z < 20) { // On the ground

      bot.botRenderer.setBranchInfo("Wall Ride");
      flatCorrectionAngle = wallRideCorrectionAngle(input, tactic);
    } else {

      // How far does the car need to rotate before it's pointing exactly at the ball?
      bot.botRenderer.setBranchInfo("Flat correction angle.");
      flatCorrectionAngle = Angles.flatCorrectionAngle(input.car, targetPosition);
    }

    output
        .withSteer(flatCorrectionAngle * 2)
        .withThrottle(1)
        .withSlide(Math.abs(flatCorrectionAngle) > 1);

    boostToShoot(input, output, flatCorrectionAngle);

    if (carToTarget.norm() > 1500
        && Math.abs(flatCorrectionAngle) < .25
        && input.car.hasWheelContact
        && ((input.car.groundSpeed > 1300 && input.car.boost < 20) || input.car.groundSpeed > 1500)) {
      tacticManager.preemptTactic(Tactic.builder()
          .setSubject(tactic.subject)
          .setTacticType(Tactic.TacticType.FLIP)
          .build());
    }

    checkComplete(tactic);
  }

  private void checkComplete(Tactic tactic) {
    Optional<BallPrediction> predictionOptional = DllHelper.getBallPrediction();
    if (predictionOptional.isPresent()) {
      BallPrediction prediction = predictionOptional.get();
      for (int i = 0 ; i < prediction.slicesLength() ; i++) {
        PredictionSlice predictionSlice = prediction.slices(i);
        if (tactic.getTargetPosition().distance(Vector3.of(predictionSlice.physics().location())) < 50) {
          return;
        }
      }

      // If the ball is no longer predicted to be at the given tactic location.
      tacticManager.setTacticComplete(tactic);
    }
  }

  private double wallRideCorrectionAngle(DataPacket input, Tactic nextTactic) {
    Vector2 carDirection = input.car.orientation.getNoseVector().flatten();
    Vector3 targetPosition = nextTactic.subject.position;
    Vector3 carToTarget = targetPosition.minus(input.car.position);

    Vector2 targetVector = targetPosition.flatten();

    // TODO(ahatfield): This only works for side walls.
    // TODO(ahatfield): Fix this. The x coordinate is positive the other way.
    // Project the height of the ball into the wall.
    float xVector = targetPosition.x > 0 ? targetPosition.z : targetPosition.z * -1;
    // If you are going down field, you need to rid up the wall sooner.
    float yVector = HALF_LENGTH - Math.abs(targetPosition.x) * (input.car.velocity.y > 0 ? 1 : -1);

    Vector2 projectedVector = new Vector2(xVector, yVector);
    Vector2 wallAdjustedVector = targetVector.plus(projectedVector);

    bot.botRenderer.renderProjection(input.car, wallAdjustedVector);

    // Determine angle with the wall.
    return Angles.flatCorrectionAngle(
        input.car.position.flatten(),
        input.car.orientation.getNoseVector().flatten(),
        wallAdjustedVector);
  }

  private void boostToShoot(DataPacket input, ControlsOutput output, double flatCorrectionAngle) {
    if ((input.car.boost > 12 || amBoosting) && Math.abs(flatCorrectionAngle) < .2) {

      double goalCorrectionAngle =
          Angles.flatCorrectionAngle(input.car.position, input.car.orientation.getNoseVector(), bot.opponentsGoal.center);

      if (goalCorrectionAngle < .2) {
        output.withBoost();
        amBoosting = true;
      } else {
        amBoosting = false;
      }
    } else {
      amBoosting = false;
    }
  }
}
