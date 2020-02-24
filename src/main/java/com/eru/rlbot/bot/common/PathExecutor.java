package com.eru.rlbot.bot.common;

import com.eru.rlbot.bot.tactics.Tactician;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PathExecutor {

  private static final Logger logger = LogManager.getLogger("PathExecutor");

  private final Tactician tactician;

  private PathExecutor(Tactician tactician) {
    this.tactician = tactician;
  }

  public static PathExecutor forTactician(Tactician tactician) {
    return new PathExecutor(tactician);
  }

  private static final double P = 50;
  private static final double D = 0.00 * Path.LEAD_FRAMES;

  public void executePath(DataPacket input, ControlsOutput output, Path path) {
    Vector3 target = path.pidTarget(input);
    Segment currentSegment = path.getSegment(input);

    Vector3 distanceDiff = target.minus(input.car.position);
    if (distanceDiff.magnitude() > Constants.BOOSTED_MAX_SPEED * 2 * Path.LEAD_TIME) {
      logger.debug("Off course: {} {}", currentSegment.type, distanceDiff.magnitude());
      path.markOffCourse();
    } else {
      double delta = path.currentTarget(input).distance(input.car.position);
      if (delta > 10 && delta < 6000) {
        logger.debug("Delta {}", delta);
      }
    }

    if (currentSegment.type == Segment.Type.FLIP) {
      flip(input, output, path);
    } else {
      drive(input, output, target, currentSegment, distanceDiff);
    }

    if (STAND_STILL) {
      output
          .withSteer(0)
          .withBoost(false)
          .withThrottle(0);
    }
  }

  private void flip(DataPacket input, ControlsOutput output, Path path) {
    if (input.car.hasWheelContact) {
      output.withJump();
    } else if (JumpManager.forCar(input.car).hasReleasedJumpInAir()) {

    } else if (JumpManager.forCar(input.car).canFlip()) {
      output.withJump()
          .withPitch(-1);
    }
  }

  private void drive(DataPacket input, ControlsOutput output, Vector3 target, Segment currentSegment, Vector3 distanceDiff) {
    // Determine the angular velocity to hit the point
    double correctionAngle = Angles.flatCorrectionAngle(input.car, target);
    double correctionCurvature = 1 / (input.car.position.distance(target) / (2 * Math.sin(correctionAngle)));
    double correctionAngularVelocity = correctionCurvature * input.car.groundSpeed;

    double maxCurvature = Constants.curvature(input.car.groundSpeed);
    double maxAngularVelocity = maxCurvature * input.car.groundSpeed;

    double currentAngularVelocity = input.car.angularVelocity.z;
    double diffAngularVelocity = currentAngularVelocity - maxAngularVelocity;

//    output.withSteer((diffAngularVelocity * P) - (currentAngularVelocity * D));
    output.withSteer(2 * correctionAngularVelocity / maxAngularVelocity);

    double timeToTarget = distanceDiff.magnitude() / input.car.velocity.magnitude();

    // Need to speed up
    if (timeToTarget > Path.LEAD_TIME) {
      Accels.AccelResult boostTime =
          Accels.boostedTimeToDistance(input.car.velocity.magnitude(), distanceDiff.magnitude());

      if (boostTime.time * 1.25 > Path.LEAD_TIME) {
        output
            .withBoost()
            .withThrottle(1.0);
      } else {
        Accels.AccelResult accelTime = Accels.timeToDistance(input.car.velocity.magnitude(), distanceDiff.magnitude());

        double savings = timeToTarget - accelTime.time;
        output.withThrottle((savings * 10) / Path.LEAD_TIME);
      }
    } else if (timeToTarget > Path.LEAD_TIME * .9) {
      output.withThrottle(0);
    } else {
      output.withThrottle(-1);
    }

    if (currentSegment.type == Segment.Type.JUMP) {
      output.withJump();
    }
  }

  // Indicates to keep the car stationary.
  private static boolean STAND_STILL = false;
}
