package com.eru.rlbot.testing;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.flags.GlobalDebugOptions;
import com.eru.rlbot.bot.renderer.BotRenderer;
import com.eru.rlbot.common.StateLogger;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.Controls;
import com.eru.rlbot.common.vector.Vector3;

/**
 * A class for iterating over various power sliding scenarios and capturing data to log.
 */
public final class PowerSlideTestRig {

  private static final float SPEED_INCREMENT = 100;
  private static final int HOLD_TICKS_INCREMENT = 1;
  private static final float STEER_INCREMENT = .2f;

  private static final float minSpeed = 2000;
  private static final int minHoldTicks = 0;
  private static final float maxSteeringAngle = 1;

  private static final float maxSpeed = 2000;
  private static final int maxHoldTicks = 100;
  private static final float minSteeringAngle = 1;

  private static float lastSpeed = maxSpeed;
  private static int lastHoldTicks = minHoldTicks;
  private static float lastSteerAngle = maxSteeringAngle;

  private static long lastTestId;

  private static boolean driveForward;
  private static boolean pullHandbreak;
  private static int holdHandbreakTicks;
  private static boolean releaseHandbreak;

  private static final double totalIterations = (long) ((((maxSpeed - minSpeed) / SPEED_INCREMENT) + 1)
      * (((maxSteeringAngle - minSteeringAngle) / STEER_INCREMENT) + 1)
      * (((maxHoldTicks - minHoldTicks) / HOLD_TICKS_INCREMENT) + 1));

  private static long count = 0;
  private static boolean finished;

  /**
   * Runs the simulation.
   */
  public static void execute(DataPacket input, Controls output) {
    if (finished) {
      BotRenderer.forIndex(input.car.serialNumber)
          .addAlertText("Testing complete");
      return;
    } else if (!GlobalDebugOptions.isStateLoggerEnabled()) {
      BotRenderer.forIndex(input.car.serialNumber)
          .addAlertText("Enable state logging to begin testing");
      return;
    }

    double percentComplete = count / totalIterations;
    BotRenderer.forIndex(input.car.serialNumber)
        .setBranchInfo(
            "Percent: %s Speed:%d Steer:%f hold:%d",
            String.format("%.2f", 100 * percentComplete), (int) input.car.groundSpeed, lastSteerAngle, lastHoldTicks);

    if (TrainingId.getId() != lastTestId) {
      lastTestId = TrainingId.getId();
      nextIteration();
      count++;
    }

    // Drive until we hit (0,0)
    if (driveForward) {
      output
          .withThrottle(input.car.groundSpeed + 7 > lastSpeed ? 0 : input.car.groundSpeed > lastSpeed ? .02 : 1)
          .withBoost(input.car.groundSpeed + 15 < lastSpeed)
          // Drive straight north
          .withSteer(Angles.flatCorrectionAngle(Vector3.zero(), input.car.velocity, Vector3.of(0, 1, 0)));

      if (input.car.position.y > 0) {
        driveForward = false;
        StateLogger.log(
            input,
            String.format(
                "Init Speed:%d Steer:%f hold:%d", (int) input.car.groundSpeed, lastSteerAngle, lastHoldTicks));
      }
    } else if (pullHandbreak) {
      output
          .withThrottle(1)
          .withSteer(lastSteerAngle)
          .withSlide();
      StateLogger.log(input, "drift");
      holdHandbreakTicks++;
      if (holdHandbreakTicks >= lastHoldTicks) {
        pullHandbreak = false;
      }
    } else if (releaseHandbreak) {
      output
          .withThrottle(1.0)
          .withBoost();
      if (input.car.angularVelocity.magnitude() < .01) {
        StateLogger.log(input, "rotation complete");
        // done
      } else {
        StateLogger.log(input, "straighten out");
      }
    }
  }

  private static void nextIteration() {
    driveForward = true;
    holdHandbreakTicks = 0;
    pullHandbreak = true;
    releaseHandbreak = true;

    lastHoldTicks += HOLD_TICKS_INCREMENT;

    if (lastHoldTicks > maxHoldTicks) {
      lastHoldTicks = minHoldTicks;
      lastSpeed -= SPEED_INCREMENT;
    }

    if (lastSpeed < minSpeed) {
      lastSpeed = maxSpeed;
      lastSteerAngle -= STEER_INCREMENT;
    }

    if (lastSteerAngle < minSteeringAngle) {
      finished = true;
    }
  }
}
