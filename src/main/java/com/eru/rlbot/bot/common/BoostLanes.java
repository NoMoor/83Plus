package com.eru.rlbot.bot.common;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.eru.rlbot.bot.path.Paths;
import com.eru.rlbot.bot.path.Segment;
import com.eru.rlbot.common.boost.BoostManager;
import com.eru.rlbot.common.boost.BoostPad;
import com.eru.rlbot.common.input.BoundingBox;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.List;
import java.util.function.Predicate;

/**
 * Represents static boost paths across the map. In this file, left refers to positive x and right to negative x.
 */
public final class BoostLanes {

  // TODO: Incorporate crossing lanes.
  private static final ImmutableList<Segment> orangeToBlueLeftCross =
      boostNumsToSegments(28, 23, 21, 17, 13);
  private static final ImmutableList<Segment> orangeToBlueRightCross =
      boostNumsToSegments(27, 22, 19, 16, 13);

  // Orange to Blue
  private static final ImmutableList<Segment> orangeToBlueLeftLane =
      boostNumsToSegments(28, 23, 21, 14, 11);
  private static final ImmutableList<Segment> orangeToBlueRightLane =
      boostNumsToSegments(27, 22, 19, 12, 10);
  private static final ImmutableList<Segment> orangeToBlueCenterLane =
      boostNumsToSegments(33, 26, 20, 13, 7);
  private static final ImmutableList<Segment> orangeToBlueLeftMidLargeBoost =
      boostNumsToSegments(18, 9);
  private static final ImmutableList<Segment> orangeToBlueRightMidLargeBoost =
      boostNumsToSegments(15, 8);
  private static final ImmutableList<Segment> blueLeftLargeBoost =
      toSegments(boost(9).addX(140), boost(4), boost(2).addY(-140));
  private static final ImmutableList<Segment> blueRightLargeBoost =
      toSegments(boost(8).addX(-140), boost(3), boost(1).addY(-140));
  private static final ImmutableList<Segment> blueHalfCircleLtR =
      boostNumsToSegments(6, 7, 5, 1);
  private static final ImmutableList<Segment> blueHalfCircleRtL =
      boostNumsToSegments(1, 5, 7, 6, 2);

  private static final ImmutableList<Segment> blueToOrangeLeftLane =
      boostNumsToSegments(6, 11, 14, 21, 23);
  private static final ImmutableList<Segment> blueToOrangeRightLane =
      boostNumsToSegments(5, 10, 12, 19, 22);
  private static final ImmutableList<Segment> blueToOrangeCenterLane =
      boostNumsToSegments(0, 7, 13, 20, 26);
  private static final ImmutableList<Segment> blueToOrangeRightMidLargeBoost =
      boostNumsToSegments(15, 24);
  private static final ImmutableList<Segment> blueToOrangeLeftMidLargeBoost =
      boostNumsToSegments(18, 25);
  private static final ImmutableList<Segment> orangeLeftLargeBoost =
      toSegments(boost(25).addX(140), boost(30), boost(32).addY(140));
  private static final ImmutableList<Segment> orangeRightLargeBoost =
      toSegments(boost(24).addX(-140), boost(29), boost(31).addY(140));
  private static final ImmutableList<Segment> orangeHalfCircleLtR =
      boostNumsToSegments(32, 28, 26, 27, 31);
  private static final ImmutableList<Segment> orangeHalfCircleRtL =
      boostNumsToSegments(31, 27, 26, 28, 32);

  private static Vector3 boost(int i) {
    BoostPad pad = BoostManager.allBoosts().get(i);
    return pad.isLargeBoost()
        ? pad.getLocation()
        .addX(Math.signum(pad.getLocation().x) * 150)
        .addY(Math.signum(pad.getLocation().y) * 150)
        : pad.getLocation();
  }

  private static ImmutableList<Segment> boostNumsToSegments(Integer... boostPadNumbers) {
    return boostNumsToSegments(ImmutableList.copyOf(boostPadNumbers));
  }

  private static ImmutableList<Segment> boostNumsToSegments(ImmutableList<Integer> boostPadNumber) {
    ImmutableList<Vector3> boostPads = boostPadNumber.stream()
        .map(BoostLanes::boost)
        .collect(toImmutableList());

    return toSegments(boostPads);
  }

  private static ImmutableList<Segment> toSegments(Vector3... wayPoints) {
    return toSegments(ImmutableList.copyOf(wayPoints));
  }

  private static ImmutableList<Segment> toSegments(ImmutableList<Vector3> wayPoints) {
    if (wayPoints.size() < 2) {
      return ImmutableList.of();
    }

    ImmutableList.Builder<Segment> segmentBuilder = ImmutableList.builder();
    for (int i = 1; i < wayPoints.size(); i++) {
      segmentBuilder.add(Segment.straight(wayPoints.get(i - 1), wayPoints.get(i)));
    }
    return segmentBuilder.build();
  }

  public static ImmutableList<Segment> preparePath(ImmutableList<Segment> segments, CarData car) {
    ImmutableList<Segment> prunedPath = prunePath(segments, car);

    return smoothPath(prunedPath);
  }

  private static ImmutableList<Segment> smoothPath(ImmutableList<Segment> prunedPath) {
    if (prunedPath.size() < 2) {
      return prunedPath;
    }

    ImmutableList.Builder<Segment> segmentBuilder = ImmutableList.builder();

    Segment lastArc = null;
    for (int i = 0; i + 1 < prunedPath.size(); i++) {
      Segment segment1 = prunedPath.get(i);
      Segment segment2 = prunedPath.get(i + 1);
      Vector3 vec1 = segment1.end.minus(segment1.start);
      Vector3 vec2 = segment2.end.minus(segment2.start);
      Vector3 vec2i = vec2.toMagnitude(-1);

      double angle = vec1.angle(vec2i);

      if (angle > 2.85) {
        // This is basically straight. Remove this way-point.
        continue;
      }

      double correctionAngle = vec1.flatten().correctionAngle(vec2.flatten());
      boolean isCcw = correctionAngle < 0;

      double r = Circle.radiusForPath(2200);
      double R = r * ((1 + Math.sin(angle / 2)) / Math.sin(angle / 2));
      double distance = Math.sqrt((R * R) - (2 * R * r));

      Vector3 apexOffset = vec1.toMagnitude(-distance);
      Vector3 dPoint = segment1.end.plus(apexOffset);
      Vector3 circleCenter = dPoint.plus(apexOffset.counterClockwisePerpendicular().toMagnitude(isCcw ? -r : r));
      Circle circle = Circle.forPath(circleCenter, r);

      Paths.TangentPoints seg1TangentPoints = Paths.tangents(circle, segment1.start);
      Vector3 turnIn = isCcw ? seg1TangentPoints.right : seg1TangentPoints.left;
      Paths.TangentPoints seg2TangentPoints = Paths.tangents(circle, segment2.end);
      Vector3 turnOut = isCcw ? seg2TangentPoints.left : seg2TangentPoints.right;

      if (lastArc == null) {
        segmentBuilder.add(Segment.straight(segment1.start, turnIn));
      } else {
        segmentBuilder.add(Segment.straight(lastArc.end, turnIn));
      }
      lastArc = Segment.arc(turnIn, turnOut, circle, !isCcw);
      segmentBuilder.add(lastArc);
    }

    if (lastArc != null) {
      segmentBuilder.add(Segment.straight(lastArc.end, Iterables.getLast(prunedPath).end));
    }

    return segmentBuilder.build();
  }

  private static ImmutableList<Segment> prunePath(ImmutableList<Segment> segments, CarData car) {
    Vector3 endNode = Iterables.getLast(segments).end;
    Segment firstSegmentAhead = null;
    boolean segmentFound = false;
    for (Segment segment : segments) {
      firstSegmentAhead = segment;
      if (segment.start.distance(endNode) < car.position.distance(endNode) + BoundingBox.frontToRj) {
        segmentFound = true;
        break;
      }
    }

    List<Segment> boostSegments = segmentFound
        ? segments.subList(segments.indexOf(firstSegmentAhead), segments.size())
        : ImmutableList.of();

    if (!boostSegments.isEmpty()) {
      return ImmutableList.<Segment>builder()
          .add(Segment.straight(car.position, boostSegments.get(0).start))
          .addAll(boostSegments)
          .build();
    } else if (isEndAhead(car).test(Iterables.getLast(segments))) {
      return ImmutableList.of(Segment.straight(car.position, Iterables.getLast(segments).end));
    }

    return ImmutableList.of();
  }

  private static Predicate<? super Segment> isEndAhead(CarData car) {
    return segment -> {
      Vector3 nose = car.orientation.getNoseVector();
      Vector3 relativeEnd = segment.end.minus(car.position);
      return relativeEnd.dot(nose) >= 0;
    };
  }

  public static ImmutableList<Segment> right(CarData car) {
    return car.team == 0
        ? orangeToBlueRightLane
        : blueToOrangeRightLane;
  }

  public static ImmutableList<Segment> left(CarData car) {
    return car.team == 0
        ? orangeToBlueLeftLane
        : blueToOrangeLeftLane;
  }

  public static ImmutableList<Segment> rightMidLarge(CarData car) {
    return car.team == 0
        ? orangeToBlueRightMidLargeBoost
        : blueToOrangeRightMidLargeBoost;
  }

  public static ImmutableList<Segment> leftMidLarge(CarData car) {
    return car.team == 0
        ? orangeToBlueLeftMidLargeBoost
        : blueToOrangeLeftMidLargeBoost;
  }

  public static ImmutableList<Segment> backLeftLarge(CarData car) {
    return car.team == 0
        ? blueLeftLargeBoost
        : orangeLeftLargeBoost;
  }

  public static ImmutableList<Segment> backRightLarge(CarData car) {
    return car.team == 0
        ? blueRightLargeBoost
        : orangeRightLargeBoost;
  }

  public static ImmutableList<Segment> center(CarData car) {
    return car.team == 0
        ? orangeToBlueCenterLane
        : blueToOrangeCenterLane;
  }

  public static ImmutableList<Segment> halfCircleLtR(CarData car) {
    return car.team == 0
        ? blueHalfCircleLtR
        : orangeHalfCircleLtR;
  }

  public static ImmutableList<Segment> halfCircleRtL(CarData car) {
    return car.team == 0
        ? blueHalfCircleRtL
        : orangeHalfCircleRtL;
  }

  private BoostLanes() {
  }
}
