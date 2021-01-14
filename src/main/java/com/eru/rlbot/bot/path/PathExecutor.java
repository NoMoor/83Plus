package com.eru.rlbot.bot.path;

import com.eru.rlbot.bot.common.Accels;
import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.maneuver.Recover;
import com.eru.rlbot.bot.renderer.BotRenderer;
import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.bot.tactics.Tactician;
import com.eru.rlbot.common.input.BoundingBox;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.Controls;
import com.eru.rlbot.common.vector.Vector3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PathExecutor {

  private static final Logger logger = LogManager.getLogger("PathExecutor");

  private final Tactician tactician;
  public static final float MIN_FLIP_SPEED = 1100f;

  private PathExecutor(Tactician tactician) {
    this.tactician = tactician;
  }

  public static PathExecutor forTactician(Tactician tactician) {
    return new PathExecutor(tactician);
  }

  public void executePath(DataPacket input, Controls output, Path path) {
    // TODO: Handle what to do when you are ahead of schedule.
    if (path.getEndTime() < input.car.elapsedSeconds) {
      path.markOffCourse();
    }

    Vector3 target = path.pidTarget(input);
    Segment currentSegment = path.getSegment(input).getRoot();
    Segment nextSegment = path.getNextTerseSegment(currentSegment);

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

    drive(input, output, target, currentSegment, nextSegment, distanceDiff);

    if (currentSegment.type == Segment.Type.STRAIGHT && nextSegment != null && nextSegment.type == Segment.Type.ARC) {
      double timeToStraightEnd = input.car.position.distance(currentSegment.end) / input.car.groundSpeed;
      double timeOnArcSegment = nextSegment.flatDistance() / input.car.groundSpeed;
      double steer = nextSegment.clockWise ? 1 : -1;
      if (timeOnArcSegment > .4
          && timeToStraightEnd < .08
          && Math.signum(input.car.angularVelocity.z) == Math.signum(steer)) {
        output
            .withSteer(steer)
            .withSlide();
      }
    }

    // TODO: Add flip into ball sometimes.
//    if (path.getExtension() != null) {
//      if (input.car.position.distance(path.getExtension().start) < 200) {
//        BotRenderer.forIndex(input.car.serialNumber).addAlertText("Flipping into ball!");
//        tactician.requestDelegate(Flip.builder()
//            .setTarget(path.getExtension().getProgress(.5))
//            .build());
//      }
//    }

    if (currentSegment.type == Segment.Type.JUMP) {
      output.withJump();
      Segment extension = path.getExtension();
      Vector3 noseVector;
      if (extension != null) {
        noseVector = extension.end.minus(extension.start);
      } else {
        noseVector = input.car.velocity;
      }

      // TODO: Figure out why this happens.
      if (!noseVector.isZero()) {
        Angles3.pointAnyDirection(input.car, noseVector, output);
      }
    }
  }

  private static final double P = 3;
  private static final double D = .1 * Path.LEAD_FRAMES;

  private void drive(DataPacket input, Controls output, Vector3 target, Segment currentSegment, Segment nextSegment, Vector3 distanceDiff) {
    // Determine the angular velocity to hit the point
    double correctionAngle = Angles.flatCorrectionAngle(input.car, target);
    double correctionCurvature = 1 / (input.car.position.distance(target) / (2 * Math.sin(correctionAngle)));
    double correctionAngularVelocity = correctionCurvature * input.car.groundSpeed;

    double maxCurvature = Constants.curvature(input.car.groundSpeed);
    double maxAngularVelocity = maxCurvature * input.car.groundSpeed;

    double currentAngularVelocity = input.car.angularVelocity.z;
    double diffAngularVelocity = correctionAngularVelocity - currentAngularVelocity;

    float segmentModifier = currentSegment.isStraight() ? .5f : 2; // Be more gentle on the steering if we are going straight.

    double p = (correctionAngularVelocity / maxAngularVelocity) * P;
    double d = (diffAngularVelocity / maxAngularVelocity) * D;

    output.withSteer(segmentModifier * (p + d));

    double timeToTarget = distanceDiff.magnitude() / input.car.velocity.magnitude();

    // Need to speed up
    if (timeToTarget > Path.LEAD_TIME) {
      Accels.AccelResult boostTime =
          Accels.boostedTimeToDistance(input.car.velocity.magnitude(), distanceDiff.magnitude());

      if (boostTime.getDuration() > Path.LEAD_TIME) {
        output
            .withBoost(!input.car.isSupersonic)
            .withThrottle(1.0);
      } else {
        Accels.AccelResult accelTime = Accels.nonBoostedTimeToDistance(input.car.velocity.magnitude(), distanceDiff.magnitude());

        double savings = timeToTarget - accelTime.getDuration();
        output.withThrottle((savings * 10) / Path.LEAD_TIME);
      }
    } else if (timeToTarget > Path.LEAD_TIME * .4) {
      output.withThrottle(0);
    } else {
      output.withThrottle(-1);
    }

    // TODO: Delegate if we are near the end of an arc segment.
    boolean hasSpeed = input.car.groundSpeed > MIN_FLIP_SPEED && !input.car.isSupersonic;
    boolean isStraight = currentSegment.type == Segment.Type.STRAIGHT;
    boolean hasTime = (currentSegment.flatDistance() / (input.car.groundSpeed + 500)) > 1.3;
    boolean straightSteer = Math.abs(output.getSteer()) < 1;
    boolean travelingForward = Math.abs(input.car.orientation.getNoseVector().angle(input.car.velocity)) < .5;
    if (hasSpeed && isStraight && hasTime && straightSteer && travelingForward) {


//      this.tactician.requestDelegate(
//          Flip.builder()
//              .setTarget(currentSegment.end)
//              .flipEarly()
//              .build());
    }
  }

  public void executeSimplePath(DataPacket input, Controls output, Tactic tactic) {
    if (!input.car.hasWheelContact) {
      tactician.requestDelegate(new Recover(tactic.subject.position));
    } else {
      double correctionAngle = Angles.flatCorrectionAngle(input.car, tactic.subject.position);
      double distanceToTarget = input.car.position.distance(tactic.subject.position) - BoundingBox.frontToRj;
      double timeToTactic = tactic.subject.time - input.car.elapsedSeconds;

      BotRenderer.forIndex(input.car.serialNumber).setBranchInfo("Dumb execute: %.2f", timeToTactic);

      double timeToTarget = distanceToTarget / input.car.groundSpeed;

      output
          .withThrottle(timeToTactic < timeToTarget ? 1 : timeToTarget * 1.1 < timeToTactic ? -1 : 0)
          .withSteer(correctionAngle * 2)
          .withBoost(Math.abs(correctionAngle) < .5 && input.car.boost > 12 && !input.car.isSupersonic && distanceToTarget > 1000 && timeToTactic < timeToTarget)
          .withSlide(Math.abs(correctionAngle) > 1 && input.car.angularVelocity.z < 3 && input.car.groundSpeed > 1000);
    }
  }
}
