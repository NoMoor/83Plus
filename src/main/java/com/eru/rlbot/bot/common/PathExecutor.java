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

  private static final float STEERING_SENSITIVITY = 30;

  public void executePath(DataPacket input, ControlsOutput output, Path path) {
    Vector3 target = path.getPIDTarget(input);

    double correctionAngle = Angles.flatCorrectionAngle(input.car, target);
    double maxCurvature = Constants.curvature(input.car.groundSpeed);

    double proposedInput = correctionAngle * STEERING_SENSITIVITY;
    double inputCurvature = ControlsOutput.clamp((float) proposedInput) * maxCurvature;

    // TODO: This may need to be negated...
    double inputAngularVelocity = inputCurvature * input.car.groundSpeed;
    double currentAngularVelocity = input.car.angularVelocity.z;

    boolean hasExtraRotation = Math.abs(currentAngularVelocity) > Math.abs(inputAngularVelocity);

    BotRenderer.forIndex(input.car.playerIndex).setBranchInfo("%s", hasExtraRotation ? "Counter Steer" : "Steer");
    // TODO: Update to account for when we need to go from straight to turning quickly.
    if (Math.signum(currentAngularVelocity) == Math.signum(inputAngularVelocity)) {
      // Correct for the extra rotation.
      output.withSteer(hasExtraRotation ? inputAngularVelocity - (currentAngularVelocity * 3) : inputAngularVelocity);
    } else {
      output.withSteer(hasExtraRotation ? Math.signum(proposedInput) : proposedInput);
    }

    Vector3 distanceDiff = target.minus(input.car.position);
    double timeToTarget = distanceDiff.magnitude() / input.car.velocity.magnitude();

    if (timeToTarget > Path.LEAD_TIME) {
      Accels.AccelResult accelTime =
          Accels.timeToDistance(input.car.velocity.magnitude(), distanceDiff.magnitude());

      if (accelTime.time < Path.LEAD_TIME * 1.5) {
        output.withThrottle(1.0);
      } else {
//        output.withBoost();
        output.withThrottle(1.0);
      }
    } else if (timeToTarget < Path.LEAD_TIME * .8) {
      output.withThrottle(0);
    } else if (timeToTarget < Path.LEAD_TIME * .4) {
      output.withThrottle(-1);
    }

    if (target.z - 40 > input.car.position.z) {
      output.withJump();
    }
  }
}
