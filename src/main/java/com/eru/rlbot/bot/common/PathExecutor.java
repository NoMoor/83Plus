package com.eru.rlbot.bot.common;

import com.eru.rlbot.bot.tactics.Tactic;
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

  // TODO: Add in minor path deviation for boosts.
  // TODO: Generalize this and add in flips.
  public static final float TIME_BUFFER = .2f;

  private static final float STEERING_SENSITIVITY = 5;

  public void executePath(DataPacket input, ControlsOutput output, Path path) {
    Vector3 target = path.getCurrentTarget(input);

    double correctionAngle = Angles.flatCorrectionAngle(input.car, target);
    double maxCurvature = Constants.curvature(input.car.groundSpeed);

    double proposedInput = correctionAngle * STEERING_SENSITIVITY;
    double inputCurvature = ControlsOutput.clamp((float) proposedInput) * maxCurvature;

    // TODO: This may need to be negated...
    double inputAngularVelocity = inputCurvature * input.car.groundSpeed;
    double currentAngularVelocity = input.car.angularVelocity.z;

    boolean hasExtraRotation = Math.abs(currentAngularVelocity) > Math.abs(inputAngularVelocity);

    BotRenderer.forIndex(input.car.playerIndex).setBranchInfo("%s", hasExtraRotation ? "Counter Steer" : "Steer");

    if (Math.signum(currentAngularVelocity) == Math.signum(inputAngularVelocity)) {
      // Correct for the extra rotation.
      output.withSteer(hasExtraRotation ? inputAngularVelocity - (currentAngularVelocity * 3) : inputAngularVelocity);
    } else {
      output.withSteer(hasExtraRotation ? Math.signum(proposedInput) : proposedInput);
    }

    Vector3 distanceDiff = target.minus(input.car.position);
    double timeToTarget = distanceDiff.magnitude() / input.car.velocity.magnitude();

    if (timeToTarget > Path.LEAD_TIME) {
      Accels.AccelResult boostedTime =
          Accels.boostedTimeToDistance(input.car.velocity.magnitude(), distanceDiff.magnitude());
      if (boostedTime.time > Path.LEAD_TIME * .9 && !input.car.isSupersonic) {
        output.withBoost();
      } else {
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

  private void oldStyle(DataPacket input, ControlsOutput output, Path path) {
    double traverseTime = path.minimumTraverseTime();
    double currentTime = input.car.elapsedSeconds;
    double pathEndTime = path.getEndTime();

    if (traverseTime + currentTime + TIME_BUFFER < pathEndTime) {
      BotRenderer.forIndex(input.car.playerIndex)
          .setBranchInfo("Pause");
      output.withThrottle(.02);
      return;
    }
    BotRenderer.forIndex(input.car.playerIndex)
        .setBranchInfo("Offset: %f", pathEndTime - (traverseTime + currentTime));

    Path.Segment nextSegment = path.getSegment(input);

    double correctionAngle = Angles.flatCorrectionAngle(input.car, nextSegment.end);
    if (nextSegment.type == Path.Segment.Type.ARC) {
      double currentMaxCurvature = Constants.curvature(input.car.groundSpeed);
      double desiredCurvature = 1 / nextSegment.circle.radius;
      correctionAngle = (nextSegment.clockWise ? 1 : -1) * desiredCurvature / currentMaxCurvature;


      // TODO: Make mini-PID here.
      // Adjust the steering based on how far you are from the intended arc.
      double distanceToCenter = input.car.position.flatten().distance(nextSegment.circle.center.flatten());
      double correctionBias = 10;

      double offCircle = Math.abs(nextSegment.circle.radius - distanceToCenter);
      double correctionRatio = (distanceToCenter > nextSegment.circle.radius ? 1 : -1) * (.2 * Math.min(offCircle / correctionBias, 1));
      correctionAngle *= 1 + correctionRatio;
      BotRenderer.forIndex(input.car.playerIndex).addDebugText("CA: %f", correctionAngle);
    }
    double minTurnRadius = Constants.radius(input.car.velocity.magnitude());

    Circle turn = Paths.turnRadius(input.car, nextSegment.end);

    boolean insideTurnRadius = turn.center.distance(nextSegment.end) < minTurnRadius;

    boolean isFacingAwayFromTarget = input.car.orientation.getNoseVector()
        .dot(nextSegment.end.minus(input.car.position)) < 0;

    boolean canGoFaster = path.canGoFaster(input.car.groundSpeed);
    boolean breakNow = path.breakNow(input.car);

    output
        .withSteer(correctionAngle)
        .withThrottle(breakNow ? -1 : !canGoFaster ? 0.02 : 1)
        .withBoost(!insideTurnRadius && !isFacingAwayFromTarget && canGoFaster && !breakNow);

    if (nextSegment.type == Path.Segment.Type.JUMP) {
      output
          .withJump();
    }

    if (!input.car.hasWheelContact) {
      Angles3.setControlsForFlatLanding(input.car, output);
    }

    // This is not a timed path. Go ahead and use advanced mechanics.
    if (!path.isTimed()) {
      double distanceToTarget = input.car.position.distance(nextSegment.end);

      // TODO: Build this timing into the calculation.
      if (((distanceToTarget > input.car.groundSpeed * 2 && nextSegment.end.z < 150) || distanceToTarget > input.car.groundSpeed * 2.5) // Distance
          && ((input.car.groundSpeed > 1300 && input.car.boost < 20) || input.car.groundSpeed > 1500) // Speed
          && !input.car.isSupersonic // Excess Speed
          && Math.abs(correctionAngle) < .25) { // Angle

        tactician.getTacticManager().preemptTactic(Tactic.builder()
            .setSubject(nextSegment.end)
            .setTacticType(Tactic.TacticType.FLIP)
            .build());
      }
    }

//    output
//        .withThrottle(0)
//        .withBoost(false);
  }
}
