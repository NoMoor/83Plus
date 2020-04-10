package com.eru.rlbot.bot.common;

import com.eru.rlbot.bot.path.Segment;
import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.Pair;
import com.eru.rlbot.common.boost.BoostManager;
import com.eru.rlbot.common.boost.BoostPad;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;
import com.eru.rlbot.common.vector.Vector3s;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

/**
 * Class to help manage paths over boosts.
 */
public final class BoostPathHelper {

  public static Vector3 getNearestPathLocation(Segment segment, BoostPad boostPad) {
    switch (segment.type) {
      case STRAIGHT:
      case JUMP:
      case FLIP:
        return Vector3s.nearestPointOnLineSegment(boostPad.getLocation(), segment.start, segment.end);
      case ARC:
        // TODO: Check if that part of the segment exists.
        Vector3 boostToCenter = segment.circle.center.minus(boostPad.getLocation());
        double distanceToArc = boostToCenter.magnitude() - segment.circle.radius;
        return boostPad.getLocation().plus(boostToCenter.toMagnitude(distanceToArc));
      default:
        throw new IllegalStateException("Unhandled type: " + segment.type);
    }
  }

  public static Vector3 getNearestBoostEdge(Segment segment, BoostPad boostPad) {
    switch (segment.type) {
      case STRAIGHT:
      case JUMP:
      case FLIP:
        return getNearestBoostEdge(segment.start, segment.end, boostPad);
      case ARC:
        return getNearestBoostEdge(segment.circle, segment.start, segment.end, boostPad);
      default:
        throw new IllegalStateException("Unhandled type: " + segment.type);
    }
  }

  public static Vector3 getNearestBoostEdge(Vector3 start, Vector3 end, BoostPad boostPad) {
    return getNearestBoostEdge(start, end, boostPad.getLocation(), boostPad.isLargeBoost());
  }

  public static Vector3 getNearestBoostEdge(Circle circle, Vector3 start, Vector3 end, BoostPad boostPad) {
    return getNearestBoostEdge(circle, start, end, boostPad.getLocation(), boostPad.isLargeBoost());
  }

  private static Vector3 getNearestBoostEdge(Circle circle, Vector3 start, Vector3 end, Vector3 boostLocation,
                                             boolean isLargeBoost) {
    // TODO: Finish implementing this
    Vector3 boostToShortestPath = Vector3.from(boostLocation, circle.center);

    double offsetMagnitude =
        Math.min(
            boostToShortestPath.magnitude(),
            isLargeBoost ? Constants.LARGE_BOOST_PICKUP_RADIUS : Constants.SMALL_BOOST_PICKUP_RADIUS);
    Vector3 pickUpOffset = boostToShortestPath.toMagnitude(offsetMagnitude);
    return boostLocation.plus(pickUpOffset);
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

  public static Optional<BoostPad> backfieldLargePad(DataPacket input) {
    return BoostManager.getLargeBoosts().stream()
        .filter(BoostPad::isActive)
        .filter(pad -> isBackField(pad, input))
        .map(pad -> Pair.of(effectiveDistance(input.car).apply(pad), pad))
        .sorted(Comparator.comparing(Pair::getFirst))
        .filter(pair -> pair.getFirst() > 1000
            || Math.abs(Angles.flatCorrectionAngle(input.car, pair.getSecond().getLocation())) < .5)
        .findFirst()
        .map(Pair::getSecond);
  }

  private static boolean isBackField(BoostPad pad, DataPacket input) {
    Vector3 ownGoal = Goal.ownGoal(input.car.team).center;
    double ballToGoal = input.ball.position.distance(ownGoal);
    double carToPad = input.car.position.distance(pad.getLocation());
    double padToGoal = input.car.position.distance(pad.getLocation());

    return ballToGoal > carToPad + padToGoal;
  }

  public static Optional<BoostPad> backfieldAnyPad(DataPacket input) {
    return BoostManager.allBoosts().stream()
        .filter(BoostPad::isActive)
        .filter(pad -> isBackField(pad, input))
        .map(pad -> Pair.of(effectiveDistance(input.car).apply(pad), pad))
        .sorted(Comparator.comparing(Pair::getFirst))
        .filter(pair -> pair.getFirst() > 1000
            || Math.abs(Angles.flatCorrectionAngle(input.car, pair.getSecond().getLocation())) < .5)
        .findFirst()
        .map(Pair::getSecond);
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

  private static Comparator<? super BoostPad> selectBoost(DataPacket input) {
    Vector2 noseVector = input.car.orientation.getNoseVector().flatten();
    Vector2 flatPosition = input.car.position.flatten();

    return (a, b) -> {
      // Angle diff in radians
      int angleValue = (int) (Math.abs(noseVector.correctionAngle(a.getLocation().flatten()))
          - Math.abs(noseVector.correctionAngle(b.getLocation().flatten())));
      // 750 units is worth a u-turn.
      int distanceValue = (int) (flatPosition.distance(a.getLocation().flatten())
          - flatPosition.distance(b.getLocation().flatten())) / 2000;
      return angleValue + distanceValue;
    };
  }

  private BoostPathHelper() {
  }
}
