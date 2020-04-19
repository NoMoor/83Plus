package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.maneuver.Maneuver;
import com.eru.rlbot.bot.renderer.BotRenderer;
import com.eru.rlbot.bot.utils.Monitor;
import com.eru.rlbot.common.Numbers;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.Controls;
import com.eru.rlbot.common.vector.Vector3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Performs a fast aerial.
 */
public class FastAerial extends Maneuver {

  private static final Logger logger = LogManager.getLogger("FastAerial");

  private final Vector3 target;

  private boolean isFinished;
  private int hasJumpedTicks;

  private Monitor monitor;

  public FastAerial(Vector3 position) {
    this.target = position;
  }

  // TODO: Measure how much velocity this adds.
  @Override
  public void execute(DataPacket input, Controls output, Tactic nextTactic) {
    BotRenderer botRenderer = BotRenderer.forCar(input.car);

    if (monitor == null) {
      monitor = Monitor.create(input);
    }

    double pitchAngle = calculatePitchAngle(input);
    boolean boost = input.car.orientation.getNoseVector().z > pitchAngle;

    JumpManager jumpManager = JumpManager.forCar(input.car);
    if (input.car.hasWheelContact) {
      botRenderer.setBranchInfo("Do jump");
      output
          .withJump(hasJumpedTicks > 1 || !JumpManager.forCar(input.car).jumpHeld())
          .withPitch(1.0)
          .withBoost(boost);

      // If we haven't taken off yet, give up.
      if (hasJumpedTicks++ > 8) {
        isFinished = true;
      }
    } else if (!jumpManager.hasMaxJumpHeight()) {
      botRenderer.setBranchInfo("Wait for height");
      output
          .withJump()
          .withBoost(boost)
          .withPitch(1.0);
    } else if (!jumpManager.hasReleasedJumpInAir()) {
      botRenderer.setBranchInfo("Release Button");
      output
          .withBoost(boost)
          .withPitch(1.0);
    } else if (jumpManager.canFlip()) {
      botRenderer.setBranchInfo("Second Jump");
      // Release Pitch for this frame.
      output
          .withJump()
          .withBoost(boost);
    } else if (input.car.doubleJumped) {
      output
          .withPitch(1)
          .withBoost(boost);
      isFinished = true;
    } else {
      logger.debug("Unexpected fall through");
    }

    monitor.trackWhile(!isFinished, input.car);
  }

  private double calculatePitchAngle(DataPacket input) {
    Vector3 carToTarget = input.car.orientation.localCoordinates(target.minus(input.car.position));

    // TODO: This is wrong but should be close enough for this purposes.
    double verticalBoostTimeToTarget = carToTarget.z / 1200;

    // Time to target. Negative means the target is behind us.
    double horizontalTimeToTarget = carToTarget.x / input.car.orientation.localCoordinates(input.car.velocity).x;

    if (horizontalTimeToTarget < 0) {
      return 1;
    } else {
      // horizontalTimeToTarget = 2 and vertical = 1, return .5
      // horizontalTimeToTarget = 1 and vertical = 1, return 1
      //
      return Numbers.clamp(verticalBoostTimeToTarget / horizontalTimeToTarget, 0, .8);
    }
  }

  @Override
  public boolean isComplete() {
    return isFinished;
  }
}
