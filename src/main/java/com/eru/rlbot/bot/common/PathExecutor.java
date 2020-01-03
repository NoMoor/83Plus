package com.eru.rlbot.bot.common;

import com.eru.rlbot.bot.tactics.Tactician;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;

public class PathExecutor {

  private final Tactician tactician;

  private PathExecutor(Tactician tactician) {
    this.tactician = tactician;
  }

  public static PathExecutor forTactician(Tactician tactician) {
    return new PathExecutor(tactician);
  }

  private static final double P = 8;
  private static final double D = 0.05 * Path.LEAD_FRAMES;

  public void executePath(DataPacket input, ControlsOutput output, Path path) {
    Vector3 target = path.updateAndGetPidTarget(input);

    Vector3 distanceDiff = target.minus(input.car.position);
    if (distanceDiff.magnitude() > Constants.BOOSTED_MAX_SPEED * 2 * Path.LEAD_TIME) {
      path.markOffCourse();
    }

    double correctionAngle = Angles.flatCorrectionAngle(input.car, target);
    double maxCurvature = Constants.curvature(input.car.groundSpeed);
    double neededCurvature = correctionAngle * maxCurvature;

    double inputAngularVelocity = neededCurvature * input.car.groundSpeed;
    double currentAngularVelocity = input.car.angularVelocity.z;

    output.withSteer((inputAngularVelocity * P) - (currentAngularVelocity * D));

    double timeToTarget = distanceDiff.magnitude() / input.car.velocity.magnitude();

    // Need to speed up
    if (timeToTarget > Path.LEAD_TIME) {
      Accels.AccelResult boostTime =
          Accels.boostedTimeToDistance(input.car.velocity.magnitude(), distanceDiff.magnitude());

      if (boostTime.time > Path.LEAD_TIME * 1.5) {
        output
            .withBoost(Math.abs(output.getSteer()) < .8)
            .withThrottle(1.0);
      } else {
        Accels.AccelResult accelTime = Accels.timeToDistance(input.car.velocity.magnitude(), distanceDiff.magnitude());

        double savings = timeToTarget - accelTime.time;
        output.withThrottle((savings * 10) / Path.LEAD_TIME);
      }
    } else if (timeToTarget > Path.LEAD_TIME * .7) {
      output.withThrottle(0);
    } else {
      output.withThrottle(-1);
    }

    if (target.z - 40 > input.car.position.z) {
      output.withJump();
    }

    if (STAND_STILL) {
      output
          .withSteer(0)
          .withBoost(false)
          .withThrottle(0);
    }
  }

  // Indicates to keep the car stationary.
  private static boolean STAND_STILL = false;
}
