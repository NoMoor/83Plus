package com.eru.rlbot.bot.tactics;

import static com.google.common.base.Preconditions.checkArgument;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.BoostPathHelper;
import com.eru.rlbot.bot.common.SupportRegions;
import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
import com.eru.rlbot.bot.maneuver.Recover;
import com.eru.rlbot.bot.maneuver.WallHelper;
import com.eru.rlbot.common.boost.BoostPad;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.Controls;
import com.eru.rlbot.common.vector.Vector3;
import java.util.Optional;

public class GuardianTactician extends Tactician {

  GuardianTactician(ApolloGuidanceComputer bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  @Override
  void internalExecute(DataPacket input, Controls output, Tactic tactic) {
    checkArgument(tactic.subject.supportingRegions != null, "Guard only works with regions.");

    SupportRegions guardingRegions = tactic.subject.supportingRegions;

    // TODO: Change the distance to a time criteria.

    if (WallHelper.isOnWall(input.car)) {
      WallHelper.drive(input, output, guardingRegions.supportLocation);
    } else if (input.car.hasWheelContact) {
      double distanceToGuardedLocation = input.car.position.distance(guardingRegions.averageLocation());
      double ballDistanceToGuardedLocation = guardingRegions.averageLocation().distance(input.ball.position);
      if (ballDistanceToGuardedLocation > 4000) {
        Optional<BoostPad> nearestPad =
            input.car.boost < 50 ? BoostPathHelper.backfieldLargePad(input) : BoostPathHelper.backfieldAnyPad(input);
        if (distanceToGuardedLocation < ballDistanceToGuardedLocation
            && nearestPad.isPresent() && input.car.boost < 70) {

          bot.botRenderer.setBranchInfo("Pick up boost");
          bot.botRenderer.renderTarget(nearestPad.get().getLocation());
          double correctionAngle = Angles.flatCorrectionAngle(input.car, nearestPad.get().getLocation());
          output
              .withThrottle(1.0)
              .withSteer(correctionAngle)
              .withSlide(Math.abs(correctionAngle) > 1.25 && Math.abs(input.car.angularVelocity.z) < 2)
              .withBoost(nearestPad.get().isLargeBoost() && Math.abs(correctionAngle) < .3);
        } else {
          faceBall(input, output, guardingRegions);
        }
      } else {
        faceBall(input, output, guardingRegions);
      }
    } else {
      delegateTo(new Recover(guardingRegions.averageLocation()));
    }
  }

  // TODO: Keep the car in between the ball and the guarded location.
  private static final double roamingDistance = 2000;

  private void faceBall(DataPacket input, Controls output, SupportRegions guardingRegions) {
    double distanceToGuardedLocation = input.car.position.distance(guardingRegions.averageLocation());
    double distanceToBall = input.car.position.distance(input.ball.position);
    double distanceGuardedLocationToBall = input.ball.position.distance(guardingRegions.averageLocation());

    boolean focusOnBall = distanceToBall < 2000 || distanceToGuardedLocation < roamingDistance;
    Vector3 target = focusOnBall
        ? input.ball.position
        : guardingRegions.averageLocation(); // Go Back to the guarded location.

    bot.botRenderer.setBranchInfo("Face the ball");
    bot.botRenderer.renderTarget(target);

    double correctionAngle = Angles.flatCorrectionAngle(input.car, target);
    double throttle = 0;
    if (focusOnBall) {
      if (Math.abs(correctionAngle) > .3) {
        throttle = 1;
      } else if (distanceToBall + 500 > distanceGuardedLocationToBall) {
        throttle = 1;
      } else if (input.car.groundSpeed > 400) {
        throttle = -1;
      }
    } else {
      if (distanceToGuardedLocation > roamingDistance) {
        throttle = 1;
      } else if (distanceToGuardedLocation < 600 && input.car.groundSpeed > 400) {
        throttle = -1;
      }
    }

    output
        .withThrottle(throttle)
        .withSteer(correctionAngle * 5)
        .withSlide(Math.abs(correctionAngle) > 1.25 && Math.abs(input.car.angularVelocity.z) < 2)
        .withBoost(throttle == 1 && input.car.boost > 90);
  }
}
