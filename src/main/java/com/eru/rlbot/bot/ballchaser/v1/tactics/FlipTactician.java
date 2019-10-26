package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;

import static com.eru.rlbot.bot.common.Constants.BALL_SIZE;

/** Handles flip tactics for front and diagonal flips. */
public class FlipTactician extends Tactician {

  private Vector3 targetPosition;

  public FlipTactician(EruBot bot) {
    super(bot);
  }

  /** Helps the strategy unit reason about things. */
  public int shouldExecute(DataPacket input) {
    return 0;
  }


  void setTarget(Vector3 targetPosition) {
    // TODO: Ensure this is called.
    this.targetPosition = targetPosition;
  }

  @Override
  public boolean execute(DataPacket input, ControlsOutput output, Tactic nextTactic) {
    double flatCorrectionAngle = Angles.flatCorrectionDirection(input.car, targetPosition);

    // Do a front flip.
    // TODO(ahatfield): Add something about making sure the ball is far enough away.
    // Is going some reasonable speed
    if (flatCorrectionAngle < .25) {

      // Is touching the ground or holding the button doesn't do anything anymore
      double heightDifference = targetPosition.z - input.car.position.z - BALL_SIZE;
      // TODO(ahatfield): This 750 should change based on the height I was at when I jumped.
      boolean canSingleJumpTo = heightDifference > input.car.velocity.z && heightDifference < 750;

      if ((input.car.hasWheelContact || !JumpManager.hasMaxJumpHeight()) && canSingleJumpTo) {
        output.withJump();
      } else if (JumpManager.canFlip()) {
        output.withJump()
            .withPitch(-1);
      }
    }

    return false;
  }
}
