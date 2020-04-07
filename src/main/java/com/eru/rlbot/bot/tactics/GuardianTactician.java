package com.eru.rlbot.bot.tactics;

import static com.google.common.base.Preconditions.checkArgument;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.common.BoostPathHelper;
import com.eru.rlbot.bot.common.SupportRegions;
import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
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
      if (guardingRegions.averageLocation().distance(input.ball.position) > 4000) {
        // TODO: if it's farther away, pick up a larger pad.
        Optional<BoostPad> nearestPad = BoostPathHelper.nearestBoostPad(input.car);
        if (distanceToGuardedLocation < 2500 && nearestPad.isPresent() && input.car.boost < 80) {
          bot.botRenderer.setBranchInfo("Pick up boost");
          bot.botRenderer.renderTarget(nearestPad.get().getLocation());
          double correctionAngle = Angles.flatCorrectionAngle(input.car, nearestPad.get().getLocation());
          output
              .withThrottle(1.0)
              .withSteer(correctionAngle)
              .withSlide(Math.abs(correctionAngle) > 1 && Math.abs(input.car.angularVelocity.z) < 3);
        } else {
          faceBall(input, output, guardingRegions);
        }
      } else {
        faceBall(input, output, guardingRegions);
      }
    } else {
      output.withThrottle(1.0);
      Angles3.setControlsForFlatLanding(input.car, output);
    }
  }

  private void faceBall(DataPacket input, Controls output, SupportRegions guardingRegions) {
    double distanceToGuardedLocation = input.car.position.distance(guardingRegions.averageLocation());
    double distanceToBall = input.car.position.distance(input.ball.position);

    Vector3 target = distanceToGuardedLocation > 2000 && distanceToBall > 2000
        ? guardingRegions.averageLocation()
        : input.ball.position;

    bot.botRenderer.setBranchInfo("Face the ball");
    bot.botRenderer.renderTarget(target);

    double correctionAngle = Angles.flatCorrectionAngle(input.car, target);
    double throttle = distanceToGuardedLocation > 1000 || Math.abs(correctionAngle) > .5
        ? 1
        : distanceToGuardedLocation < 600 && input.car.groundSpeed > 400
        ? -1
        : 0;

    output
        .withThrottle(throttle)
        .withSteer(correctionAngle * 5)
        .withSlide(Math.abs(correctionAngle) > 1 && Math.abs(input.car.angularVelocity.z) < 3);
  }
}
