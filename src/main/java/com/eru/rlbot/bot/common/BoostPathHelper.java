package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.Pair;
import com.eru.rlbot.common.boost.BoostManager;
import com.eru.rlbot.common.boost.BoostPad;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.vector.Vector3;
import com.eru.rlbot.common.vector.Vector3s;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

/**
 * Class to help manage paths over boosts.
 */
public final class BoostPathHelper {

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

  public static Optional<BoostPad> nearestBoostPad(CarData car) {
    return BoostManager.allBoosts().stream()
        .filter(BoostPad::isActive)
        .map(pad -> Pair.of(effectiveDistance(car).apply(pad), pad))
        .sorted(Comparator.comparing(Pair::getFirst))
        .filter(pair -> pair.getFirst() > 1000
            || Math.abs(Angles.flatCorrectionAngle(car, pair.getSecond().getLocation())) < .3)
        .findFirst()
        .map(Pair::getSecond);
  }

  public static Optional<BoostPad> boostOnTheWay(CarData car, Vector3 target) {
    double distanceToTarget = car.position.distance(target);
    Vector3 carTarget = target.minus(car.position);

    return BoostManager.allBoosts().stream()
        .filter(BoostPad::isActive)
        .map(pad -> Pair.of(effectiveDistance(car).apply(pad), pad))
        .sorted(Comparator.comparing(Pair::getFirst))
        .filter(pair -> pair.getFirst() < distanceToTarget * .75) // Is between me and the target.
        .filter(pair -> // Is shallow angle pickup
            Math.abs(Angles.flatCorrectionAngle(car.position, carTarget, pair.getSecond().getLocation())) < .075)
        .findFirst()
        .map(Pair::getSecond);
  }

  private static Function<BoostPad, Double> effectiveDistance(CarData car) {
    return boostPad -> {
      double actualDistance = boostPad.getLocation().distance(car.position);
      double angleChange = Math.abs(Angles.flatCorrectionAngle(car, boostPad.getLocation()));

      // Reduce the cost of boosts that are approximately in front of us.
//      double dulledAngleChange = Math.min(0, angleChange - .2);
      double angleChangeDistance = angleChange * Constants.radius(car.groundSpeed);
      return actualDistance + (angleChangeDistance * 2);
    };
  }

  private BoostPathHelper() {
  }
}
