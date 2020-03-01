package com.eru.rlbot.bot.strats;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.boost.BoostManager;
import com.eru.rlbot.common.boost.BoostPad;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;
import com.eru.rlbot.common.vector.Vector3s;
import com.google.common.collect.ImmutableList;
import java.util.Optional;

/**
 * Helps plan paths by adding way points and boost pickup.
 */
public class LegacyPathPlanner {

  private DataPacket input;

  LegacyPathPlanner(DataPacket input) {
    this.input = input;
  }

  public ImmutableList<Tactic> plan(Tactic tactic) {
    ImmutableList.Builder<Tactic> pathBuilder = ImmutableList.builder();

    Optional<BoostPad> optionalBoostPad = closestBoostPad(tactic, 250);
    optionalBoostPad.map(pad -> Tactic.builder()
        .setSubject(getNearestBoostEdgeMoment(
            input.car.position, tactic.subject.position, pad.getLocation(), pad.isLargeBoost()))
        .setTacticType(tactic.tacticType)
        .setTacticType(Tactic.TacticType.ROTATE)
        .setObject(tactic.subject.position)
        .build())
        .ifPresent(pathBuilder::add);

    return pathBuilder
        .add(tactic)
        .build();
  }


  public static Vector3 getNearestBoostEdge(Vector3 start, Vector3 end, BoostPad boostPad) {
    return getNearestBoostEdge(start, end, boostPad.getLocation(), boostPad.isLargeBoost());
  }

  public static Vector3 getNearestBoostEdge(
      Vector3 pathStart, Vector3 pathEnd, Vector3 boostLocation, boolean isLargeBoost) {

    Vector3 boostToShortestPath =
        Vector3.from(boostLocation, Vector3s.nearestPointOnLineSegment(boostLocation, pathStart, pathEnd));

    double offsetMagnitude =
        Math.min(
            boostToShortestPath.magnitude(),
            isLargeBoost ? Constants.LARGE_BOOST_PICKUP_RADIUS : Constants.SMALL_BOOST_PICKUP_RADIUS);

    Vector3 pickUpOffset = boostToShortestPath.toMagnitude(offsetMagnitude);
    return boostLocation.plus(pickUpOffset);
  }

  public static Moment getNearestBoostEdgeMoment(
      Vector3 pathStart, Vector3 pathEnd, Vector3 boostLocation, boolean isLargeBoost) {
    return new Moment(
        getNearestBoostEdge(pathStart, pathEnd, boostLocation, isLargeBoost),
        isLargeBoost ? Moment.Type.LARGE_BOOST : Moment.Type.SMALL_BOOST);
  }

  private Optional<BoostPad> closestBoostPad(Tactic tactic, int maxOutOfWayTravel) {
    return closestBoostPad(input, tactic, maxOutOfWayTravel);
  }

  public static Optional<BoostPad> closestBoostPad(DataPacket input, Tactic tactic, int maxOutOfWayTravel) {
    // TODO: Check if we need boost (< 80 boost or will use boost on the maneuver)
    // TODO: Take into account boost size
    BoostPad closetBoost = null;
    double shortestDistance = maxOutOfWayTravel;

    Vector3 carPosition = input.car.position;
    Vector3 tacticPosition = tactic.subject.position;
    for (BoostPad boostPad : BoostManager.allBoosts()) {
      if (!boostPad.isActive()) {
        continue;
      }
      double pickupRadius = boostPad.isLargeBoost()
          ? Constants.LARGE_BOOST_PICKUP_RADIUS
          : Constants.SMALL_BOOST_PICKUP_RADIUS;

      double thisDistance =
          Math.max(
              Vector3s.shortestDistanceToVector(boostPad.getLocation(), carPosition, tacticPosition) - pickupRadius,
              0);

      if (thisDistance < shortestDistance) {
        // Ensure the boost is good to pick up.
        Moment pickupLocation =
            getNearestBoostEdgeMoment(carPosition, tacticPosition, boostPad.getLocation(), boostPad.isLargeBoost());

        boolean isTooCloseToTactic = tacticPosition.distance(pickupLocation.position) < maxOutOfWayTravel * 2;

        // TODO: This should depend on speed, distance, etc.
        boolean isTooWideToCar =
            Math.abs(Angles.flatCorrectionAngle(carPosition, input.car.orientation.getNoseVector(), pickupLocation.position)) > .2;
        boolean isTooWideToTactic =
            Math.abs(Angles.flatCorrectionAngle(
                pickupLocation.position,
                Vector3.from(input.car.position, pickupLocation.position),
                tacticPosition)) > .2;

        if (!isTooCloseToTactic && !isTooWideToCar && !isTooWideToTactic) {
          shortestDistance = thisDistance;
          closetBoost = boostPad;
        }
      }
    }
    return Optional.ofNullable(closetBoost);
  }
}
