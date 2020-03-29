package com.eru.rlbot.bot.tactics;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.common.BoostPathHelper;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
import com.eru.rlbot.bot.maneuver.WallHelper;
import com.eru.rlbot.bot.path.Path;
import com.eru.rlbot.bot.path.PathPlanner;
import com.eru.rlbot.bot.path.Segment;
import com.eru.rlbot.common.Pair;
import com.eru.rlbot.common.boost.BoostManager;
import com.eru.rlbot.common.boost.BoostPad;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.output.Controls;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;
import java.awt.Color;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Moves the ball back toward the given location.
 */
public class RotateTactician extends Tactician {

  RotateTactician(ApolloGuidanceComputer bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  public static boolean shouldRotateBack(DataPacket input) {
    double carToGoal = input.car.position.distance(Goal.ownGoal(input.car.team).center);
    double ballToGoal = input.ball.position.distance(Goal.ownGoal(input.car.team).center);

    return ballToGoal < carToGoal;
  }

  @Override
  void internalExecute(DataPacket input, Controls output, Tactic tactic) {
    usingPathPlanner(input, output, tactic);
  }

  private boolean locked;

  @Override
  public boolean isLocked() {
    return locked;
  }

  private void usingPathPlanner(DataPacket input, Controls output, Tactic tactic) {
    if (WallHelper.isOnWall(input.car)) {
      bot.botRenderer.setBranchInfo("Get off the wall");

      WallHelper.drive(input, output, input.ball.position);
      return;
    }


    Vector3 rotationDirection = tactic.object.minus(tactic.subject.position);
    CarData car = input.car.toBuilder()
        .setOrientation(Orientation.fromFlatVelocity(rotationDirection))
        .setVelocity(rotationDirection.toMagnitude(Math.min(input.car.velocity.magnitude() + 100, Constants.SUPER_SONIC)))
        .setPosition(tactic.subject.position)
        .build();

    Path path = PathPlanner.planPath(input.car, car);

    ImmutableList<Pair<BoostPad, Vector3>> pads = boostsNearPath(path);
    Optional<Pair<BoostPad, Vector3>> closetsPad = pads.stream()
        .min(Comparator.comparing(pair -> pair.getFirst().getLocation().distance(pair.getSecond())));

    if (closetsPad.isPresent() && input.car.boost < 50) {
      Pair<BoostPad, Vector3> pad = closetsPad.get();
      bot.botRenderer.renderTarget(Color.MAGENTA, pad.getFirst().getLocation());

      Vector3 pickupLocation = getNearestPickup(pad);
      Vector3 travelDirection = tactic.subject.position.minus(pickupLocation);
      CarData boostPadCar = input.car.toBuilder()
          .setOrientation(Orientation.fromFlatVelocity(travelDirection))
          .setVelocity(travelDirection.toMagnitude(Math.min(input.car.velocity.magnitude() + 100, Constants.SUPER_SONIC)))
          .setPosition(pickupLocation)
          .build();
      bot.botRenderer.renderPath(Color.CYAN, path);
      path = PathPlanner.planPath(input.car, boostPadCar);
    }

    path.lockAndSegment(false);
    bot.botRenderer.renderPath(input, path);

    if (input.car.hasWheelContact) {
      pathExecutor.executePath(input, output, path);
    } else {
      Angles3.setControlsForFlatLanding(input.car, output);
      output.withThrottle(1.0);
    }
  }

  private ImmutableList<Pair<BoostPad, Vector3>> boostsNearPath(Path path) {
    ImmutableList<Segment> arcs = path.allTerseNodes().stream()
        .filter(segment -> segment.type == Segment.Type.ARC)
        .collect(toImmutableList());

    return BoostManager.allBoosts().stream()
        .map(pairWithClosestPickup(path))
        .filter(filterInsideTurns(arcs))
        .filter(pair -> pair.getFirst().getLocation().distance(path.getTarget().position) > 500)
        .filter(pair -> pair.getFirst().getLocation().distance(pair.getSecond()) < 500) // Don't go too far out of our way.
        .collect(toImmutableList());
  }

  private Predicate<Pair<BoostPad, Vector3>> filterInsideTurns(ImmutableList<Segment> arcs) {
    return pair -> {
      Vector3 nearestPickupLocation = getNearestPickup(pair);

      return arcs.stream()
          .noneMatch(arc -> nearestPickupLocation.distance(arc.circle.center) < arc.circle.radius - 20);
    };
  }

  private Vector3 getNearestPickup(Pair<BoostPad, Vector3> pair) {
    Vector3 pathOffsetVector = pair.getFirst().getLocation().minus(pair.getSecond()).flat();
    double offsetMagnitude =
        Math.min(
            pathOffsetVector.magnitude(),
            pair.getFirst().isLargeBoost() ? Constants.LARGE_BOOST_PICKUP_RADIUS : Constants.SMALL_BOOST_PICKUP_RADIUS);
    pathOffsetVector = pathOffsetVector.toMagnitudeUnchecked(pathOffsetVector.magnitude() - offsetMagnitude);

    return pair.getSecond().plus(pathOffsetVector);
  }

  private Function<BoostPad, Pair<BoostPad, Vector3>> pairWithClosestPickup(Path path) {
    ImmutableList<Segment> segments = path.allTerseNodes();
    return boostPad -> {
      Vector3 nearestPickup = segments.stream()
          .filter(segment -> segment.type != Segment.Type.ARC)
          .map(segment -> BoostPathHelper.getNearestPathLocation(segment, boostPad))
          .min(Comparator.comparing(pickUpLocation -> pickUpLocation.distance(boostPad.getLocation())))
          .get();

      return Pair.of(boostPad, nearestPickup);
    };
  }

  @Override
  public boolean allowDelegate() {
    return true;
  }
}
