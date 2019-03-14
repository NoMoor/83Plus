package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.common.BotRenderer;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.Bot;

import static com.eru.rlbot.bot.common.Constants.FIELD_SIZE;

public class RollingTactician implements Tactician {

  private final BotRenderer botRenderer;

  RollingTactician(Bot bot) {
    this.botRenderer = BotRenderer.forBot(bot);
  }

  @Override
  public void execute(ControlsOutput output, DataPacket input, Tactic nextTactic) {
    Vector2 carDirection = input.car.orientation.noseVector.flatten();
    Vector3 targetPosition = nextTactic.target.position;

    // Subtract the two positions to get a vector pointing from the car to the ball.
    Vector3 carToTarget = targetPosition.minus(input.car.position);

    double flatCorrectionAngle;
    // Need to go up the wall.
    if (input.car.hasWheelContact && Math.abs(carToTarget.z) > 500 && Math.abs(input.car.position.x) > 2000 && input.car.position.z < 20) {
      Vector2 targetVector = targetPosition.flatten();

      // TODO(ahatfield): This only works for side walls.
      // Figure out the x/y component of the vector to add. This is based on projecting the ball out into the wall.
      float xVector = targetPosition.x > 0 ? targetPosition.z : targetPosition.z * -1;
      // If you are going down field, you need to rid up the wall sooner.
      float yVector = FIELD_SIZE - Math.abs(targetPosition.x) * (input.car.velocity.y > 0 ? 1 : -1);

      Vector2 projectedVector = new Vector2(xVector, yVector);
      Vector2 wallAdjustedVector = targetVector.plus(projectedVector);
      botRenderer.renderProjection(input.car, wallAdjustedVector);

      // Determine angle with the wall.
      flatCorrectionAngle = -1 * carDirection.correctionAngle(wallAdjustedVector);
    } else {
      // How far does the car need to rotate before it's pointing exactly at the ball?
      flatCorrectionAngle = -1 * carDirection.correctionAngle(carToTarget.flatten());
    }

    output.withSteer((float) flatCorrectionAngle)
        .withSlide(Math.abs(flatCorrectionAngle) > 2)
        .withThrottle(1);
  }
}
