package com.eru.rlbot.bot.tactics;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.common.BoostLanes;
import com.eru.rlbot.bot.common.BoostPathHelper;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.common.Teams;
import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
import com.eru.rlbot.bot.maneuver.WallHelper;
import com.eru.rlbot.bot.path.Path;
import com.eru.rlbot.bot.path.PathPlanner;
import com.eru.rlbot.bot.path.Segment;
import com.eru.rlbot.bot.prediction.CarLocationPredictor;
import com.eru.rlbot.bot.strats.Rotations;
import com.eru.rlbot.common.Pair;
import com.eru.rlbot.common.boost.BoostManager;
import com.eru.rlbot.common.boost.BoostPad;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.output.Controls;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Moves the ball back toward the given location.
 */
public class RotateTactician extends Tactician {

  private boolean useBoostLanes;
  private boolean locked;

  private Path path;

  RotateTactician(ApolloGuidanceComputer bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  @Override
  void internalExecute(DataPacket input, Controls output, Tactic tactic) {
    locked = Rotations.get(input).getFirstMan() == input.car && Teams.getTeamSize(input.car.team) > 1;
    if (WallHelper.isOnWall(input.car)) {
      bot.botRenderer.setBranchInfo("Get off the wall");

      WallHelper.drive(input, output, input.ball.position);
    } else if (!input.car.hasWheelContact) {
      Angles3.setControlsForFlatLanding(input.car, output);
      output.withThrottle(1.0);
    } else {
      if (useBoostLanes) {
        bot.botRenderer.setBranchInfo("Boost lanes");
        usingBoostLanes(input, output, tactic);
      } else {
        bot.botRenderer.setBranchInfo("Dynamic Planner");
        usingPathPlanner(input, output, tactic);
      }

      if (tactic.subject.supportingRegions != null) {
        bot.botRenderer.renderRegions(tactic.subject.supportingRegions.regions);
      }
    }
  }

  private void usingBoostLanes(DataPacket input, Controls output, Tactic tactic) {
    if (path == null || path.isOffCourse()) {
      // TODO: Incorporate tactic.
      ImmutableList<Segment> boostLane = chooseBoostLane(input);

      ImmutableList<Segment> segments = BoostLanes.preparePath(boostLane, input.car);

      if (segments.isEmpty()) {
        // Make sure we don't get stuck.
        output.withThrottle(1.0);
        return;
      }

      path = makePath(segments, input);
      path.lockAndSegment(false);
    }

    bot.botRenderer.renderPath(input, path);
    pathExecutor.executePath(input, output, path);


    output
        .withThrottle(1.0)
        .withBoost(isPressured(input) && !input.car.isSupersonic);
  }

  private static boolean isPressured(DataPacket input) {
    Goal ownGoal = Goal.ownGoal(input.car.team);
    double carDistanceToGoal = ownGoal.center.distance(input.car.position);
    double ballDistanceToGoal = ownGoal.center.distance(input.ball.position);
    return ballDistanceToGoal - carDistanceToGoal < 1000;
  }

  @Override
  protected void reset(DataPacket input) {
    super.reset(input);

    path = null;
    useBoostLanes = input.car.boost < 50;
  }

  private Path makePath(ImmutableList<Segment> boostLane, DataPacket input) {
    Vector3 direction = Iterables.getLast(boostLane).end.minus(Iterables.getLast(boostLane).start);
    CarData endCar = CarData.builder()
        .setVelocity(direction.toMagnitude(2000))
        .setOrientation(Orientation.fromFlatVelocity(direction))
        .setPosition(Iterables.getLast(boostLane).end)
        .build();

    return Path.builder()
        .addEarlierSegments(boostLane)
        .setStartingCar(input.car)
        .setTargetCar(endCar)
        .build();
  }

  private ImmutableList<Segment> chooseBoostLane(DataPacket input) {
    Goal opponentGoal = Goal.opponentGoal(input.car.team);

    Vector3 predictedLocation = CarLocationPredictor.forCar(input.car).predictionForCar(input.car).in(.1);

    boolean carFacingLeft = input.car.orientation.getNoseVector().x > 0;
    boolean carForwardField = Math.signum(predictedLocation.y + (Math.signum(opponentGoal.center.y) * 2000))
        == Math.signum(opponentGoal.center.y);
    boolean ownHalf = Math.signum(predictedLocation.y) != Math.signum(opponentGoal.center.y);
    boolean carPositionLeft = predictedLocation.x > 0;
    boolean carFacingAway = input.car.orientation.getNoseVector().y == Math.signum(opponentGoal.center.y);

    if (Math.abs(predictedLocation.x) < 500
        && !carFacingAway
        && Math.abs(input.car.orientation.getNoseVector().y) > .6) {
      return BoostLanes.center(input.car);
    }

    // TODO: Make these work for orange.
    // TODO: This might work better with facing inward / outward.
    if (carFacingLeft) {
      if (carForwardField) {
        if (carPositionLeft) {
          // Left facing left forward
          if (predictedLocation.x > 2500) {
            return ownHalf ? BoostLanes.backLeftLarge(input.car) : BoostLanes.leftMidLarge(input.car);
          } else {
            return BoostLanes.left(input.car);
          }
        } else {
          // Right facing left forward
          return predictedLocation.x < -2500
              ? BoostLanes.backRightLarge(input.car)
              : predictedLocation.x < -1200
              ? BoostLanes.right(input.car)
              : BoostLanes.center(input.car);
        }
      } else {
        if (carPositionLeft) {
          // Left facing left backfield
          return BoostLanes.backLeftLarge(input.car);
        } else {
          // Right facing right backfield
          return predictedLocation.x > -3000 ? BoostLanes.halfCircleRtL(input.car) : BoostLanes.backRightLarge(input.car);
        }
      }
    } else {
      if (carForwardField) {
        if (carPositionLeft) {
          // Left facing right forward
          return predictedLocation.x > 2500
              ? BoostLanes.backLeftLarge(input.car)
              : predictedLocation.x > 1200
              ? BoostLanes.left(input.car)
              : BoostLanes.center(input.car);
        } else {
          // Right facing right forward
          if (predictedLocation.x < -2500) {
            return ownHalf ? BoostLanes.backRightLarge(input.car) : BoostLanes.rightMidLarge(input.car);
          } else {
            return BoostLanes.right(input.car);
          }
        }
      } else {
        if (carPositionLeft) {
          // Left facing right backfield
          return predictedLocation.x < 3000 ? BoostLanes.halfCircleLtR(input.car) : BoostLanes.backLeftLarge(input.car);
        } else {
          // Right facing right backfield
          return BoostLanes.backRightLarge(input.car);
        }
      }
    }
  }

  @Override
  public boolean isLocked() {
    return locked || super.isLocked();
  }

  private void usingPathPlanner(DataPacket input, Controls output, Tactic tactic) {
    Vector3 rotationDirection = tactic.object.minus(tactic.subject.position);
    if (rotationDirection.isZero()) {
      rotationDirection = tactic.subject.position.minus(input.car.position);
    }
    CarData targetRotation = input.car.toBuilder()
        .setOrientation(Orientation.fromFlatVelocity(rotationDirection))
        .setVelocity(rotationDirection.toMagnitude(Math.min(input.car.velocity.magnitude() + 100, Constants.SUPER_SONIC)))
        .setPosition(tactic.subject.position)
        .build();

    if (shouldRecomputePath(path)) {
      Path newPath = PathPlanner.planPath(input.car, targetRotation);

      newPath.lockAndSegment(false);
      path = newPath;
    }

    Rotations rotations = Rotations.get(input);
    bot.botRenderer.renderPath(input, path);

    pathExecutor.executePath(input, output, path);

    if (input.car.isSupersonic) {
      // Don't overwrite the boost setting.
    } else if (input.car.groundSpeed < 1800 && input.car.boost > 20) {
      output.withBoost(true);
    } else if (rotations.isLastManBack()) {
      output.withBoost(true);
    }
  }

  private boolean shouldRecomputePath(Path path) {
    return path == null || path.isOffCourse();
  }

  private ImmutableList<Pair<BoostPad, Vector3>> boostsNearPath(Path path) {
    ImmutableList<Segment> arcs = path.allTerseNodes().stream()
        .filter(segment -> segment.type == Segment.Type.ARC)
        .collect(toImmutableList());

    return BoostManager.allBoosts().stream()
        .map(pairWithClosestPickup(path))
        .filter(filterInsideTurns(arcs))
        .filter(pair -> pair.getFirst().getLocation().distance(path.getTarget().position) > 500)
        .filter(pair -> pair.getFirst().getLocation().distance(pair.getSecond()) < 300) // Don't go too far out of our way.
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
