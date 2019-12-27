package com.eru.rlbot.bot.strats;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.common.boost.BoostManager;
import com.eru.rlbot.common.boost.BoostPad;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;
import java.util.Optional;

/** Helps plan paths by adding way points and boost pickup. */
public class PathPlanner {

  private DataPacket input;

  PathPlanner(DataPacket input) {
    this.input = input;
  }

  public ImmutableList<Tactic> plan(Tactic tactic) {
    ImmutableList.Builder<Tactic> pathBuilder = ImmutableList.builder();

    Optional<BoostPad> optionalBoostPad = closestBoostPad(tactic, 250);
    optionalBoostPad
        .map(pad -> Tactic.builder()
            .setSubject(getNearestBoostEdge(
                pad.getLocation(), input.car.position, tactic.subject.position, pad.isLargeBoost()))
            .setTacticType(tactic.tacticType)
            .setSubjectType(pad.isLargeBoost() ? Tactic.SubjectType.LARGE_BOOST : Tactic.SubjectType.SMALL_BOOST)
            .setTacticType(Tactic.TacticType.ROTATE)
            .setObject(tactic.subject.position)
            .build())
        .ifPresent(pathBuilder::add);

    return pathBuilder
        .add(tactic)
        .build();
  }

  private Vector3 getNearestBoostEdge(Vector3 location, Vector3 start, Vector3 end, boolean isLargeBoost) {
    Vector3 boostToShortestPath = Vector3.from(location, nearestPointOnLineSegment(location, start, end));

    double offsetMagnitude =
        Math.min(
            boostToShortestPath.magnitude(),
            isLargeBoost ? Constants.LARGE_BOOST_PICKUP_RADIUS : Constants.SMALL_BOOST_PICKUP_RADIUS);

    Vector3 pickUpOffset = boostToShortestPath.toMagnitude(offsetMagnitude);
    return location.plus(pickUpOffset);
  }

  private Optional<BoostPad> closestBoostPad(Tactic tactic, int maxOutOfWayTravel) {
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
              shortestDistanceToVector(boostPad.getLocation(), carPosition, tacticPosition) - pickupRadius,
              0);

      if (thisDistance < shortestDistance) {
        // Ensure the boost is good to pick up.
        Vector3 pickupLocation =
            getNearestBoostEdge(boostPad.getLocation(), carPosition, tacticPosition, boostPad.isLargeBoost());

        boolean isTooCloseToTactic = tacticPosition.distance(pickupLocation) < maxOutOfWayTravel * 2;

        // TODO: This should depend on speed, distance, etc.
        boolean isTooWideToCar =
            Math.abs(Angles.flatCorrectionAngle(carPosition, input.car.orientation.getNoseVector(), pickupLocation)) > .2;
        boolean isTooWideToTactic =
            Math.abs(Angles.flatCorrectionAngle(
                pickupLocation, Vector3.from(input.car.position, pickupLocation), tacticPosition)) > .2;

        if (!isTooCloseToTactic && !isTooWideToCar && !isTooWideToTactic) {
          shortestDistance = thisDistance;
          closetBoost = boostPad;
        }
      }
    }
    return Optional.ofNullable(closetBoost);
  }

  // https://math.stackexchange.com/a/2193733
  private double shortestDistanceToVector(Vector3 location, Vector3 start, Vector3 end) {
    Vector3 closestPoint = nearestPointOnLineSegment(location, start, end);
    if (closestPoint.equals(start) || closestPoint.equals(end)) {
      return Double.MAX_VALUE;
    }
    return location.distance(closestPoint);
  }

  private Vector3 nearestPointOnLineSegment(Vector3 location, Vector3 start, Vector3 end) {
    Vector3 v = end.minus(start);
    Vector3 u = start.minus(location);

    double t = - (v.dot(u)) / (v.dot(v));
    if (t >= 0 && t <= 1) {
      return start.multiply(1 - t).plus(end.multiply(t));
    } else {
      return start.distance(location) < end.distance(location) ? start : end;
    }
  }
}
