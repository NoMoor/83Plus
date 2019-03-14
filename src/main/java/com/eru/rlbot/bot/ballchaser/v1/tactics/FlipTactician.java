package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;

import static com.eru.rlbot.bot.common.Constants.BALL_SIZE;

/** Handles flip tactics for front and diagonal flips. */
public class FlipTactician implements Tactician {

  private Vector3 targetPosition;

  /** Helps the strategy unit reason about things. */
  public int shouldExecute(DataPacket input) {
    return 0;
  }


  void setTarget(Vector3 targetPosition) {
    // TODO: Ensure this is called.
    this.targetPosition = targetPosition;
  }

  @Override
  public void execute(ControlsOutput output, DataPacket input, Tactic nextTactic) {
    Vector2 carDirection = input.car.orientation.noseVector.flatten();

    // Subtract the two positions to get a vector pointing from the car to the ball.
    Vector3 carToTarget = targetPosition.minus(input.car.position);

    // How far does the car need to rotate before it's pointing exactly at the ball?
    double flatCorrectionAngle = -1 * carDirection.correctionAngle(carToTarget.flatten());

    // Do a front flip.
    // TODO(ahatfield): Add something about making sure the ball is far enough away.
    // Is going some reasonable speed
    if (flatCorrectionAngle < .25) {

      // Is touching the ground or holding the button doesn't do anything anymore
      double heightDifference = targetPosition.z - input.car.position.z - BALL_SIZE;
      // TODO(ahatfield): This 750 should change based on the height I was at when I jumped.
      boolean canSingleJumpTo = heightDifference > input.car.velocity.z && heightDifference < 750;

      if ((input.car.hasWheelContact || !JumpManager.hasMaxJumpHight()) && canSingleJumpTo) {
        output.withJump();
      } else if (JumpManager.canDodge()) {
        output.withJump()
            .withPitch(-1);
      }
    }
  }
}
