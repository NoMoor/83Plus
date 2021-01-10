package com.eru.rlbot.bot.lookup;

import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AerialLookUp {

  private static final Logger logger = LogManager.getLogger("AerialLookUp");

  // Height rounded to nearest 50
  private static final TreeMap<Long, List<AerialInfo>> APEX_MAP = new TreeMap<>();

  /**
   * Maps from <boost,height> to a list of ways to get to that boost / height.
   */
  private static final Table<Long, Long, List<AerialInfo>> BOOST_HEIGHT_LOOKUP = HashBasedTable.create();

  public static final Vector3 FAST_AERIAL_VELOCITY = Vector3.of(0, 50, 690);
  public static double FAST_AERIAL_TIME = .25;
  public static double FAST_AERIAL_BOOST = FAST_AERIAL_TIME * Constants.BOOST_RATE;
  public static double AERIAL_EFFICIENCY = .25;

  static {
    if (false) {
      for (double r = .75; r <= Math.PI / 2; r += .1) {
        for (int i = 5; i <= 100; i++) {
          AerialInfo result = computeAerialResult(r, i);

          APEX_MAP.computeIfAbsent(toKey(result.height), (key) -> new ArrayList<>()).add(result);
        }
      }
    }
  }

  public static AerialInfo minBoost(double height) {
    List<AerialInfo> aerialInfos = APEX_MAP.ceilingEntry(toKey(height)).getValue();
    return Iterables.getLast(aerialInfos);
  }

  public static AerialInfo minAngle(double height) {
    List<AerialInfo> aerialInfos = APEX_MAP.ceilingEntry(toKey(height)).getValue();
    return Iterables.getFirst(aerialInfos, null);
  }

  public static AerialInfo averageBoost(double height) {
    List<AerialInfo> aerialInfos = APEX_MAP.ceilingEntry(toKey(height)).getValue();
    return aerialInfos.get(aerialInfos.size() / 2);
  }

  private static long toKey(double height) {
    return roundDown(height, 50);
  }

  private static long roundDown(double value, long roundTo) {
    return Math.round(value / roundTo) * roundTo;
  }

  private static AerialInfo computeAerialResult(final double boostAngle, final double initialBoostAmount) {
    double time = FAST_AERIAL_TIME;
    double y = 0;
    double z = 0;
    double remainingBoost = initialBoostAmount - FAST_AERIAL_BOOST;

    Vector3 velocity = FAST_AERIAL_VELOCITY;
    Vector3 stepVelocity =
        Vector3.of(
            0,
            Math.cos(boostAngle) * Constants.BOOSTED_ACCELERATION * Constants.STEP_SIZE,
            Math.sin(boostAngle) * Constants.BOOSTED_ACCELERATION * Constants.STEP_SIZE);
    Vector3 gravity =
        Vector3.of(
            0,
            0,
            Constants.NEG_GRAVITY * Constants.STEP_SIZE);
    stepVelocity = stepVelocity.plus(gravity);

    while (remainingBoost > 0) {
      remainingBoost -= Constants.BOOST_RATE * Constants.STEP_SIZE;
      time += Constants.STEP_SIZE;
      velocity = velocity.plus(stepVelocity);
      y += velocity.y * Constants.STEP_SIZE;
      z += velocity.z * Constants.STEP_SIZE;

      registerAerialInfo(new AerialInfo(
          boostAngle, initialBoostAmount, initialBoostAmount - remainingBoost, time, z, y));
    }

    // Coast to max height
    while (velocity.z > 0) {
      time += Constants.STEP_SIZE;
      velocity = velocity.plus(gravity);
      y += velocity.y * Constants.STEP_SIZE;
      z += velocity.z * Constants.STEP_SIZE;

      registerAerialInfo(new AerialInfo(
          boostAngle, initialBoostAmount, initialBoostAmount - remainingBoost, time, z, y));
    }

    AerialInfo result = new AerialInfo(
        boostAngle, initialBoostAmount, initialBoostAmount - remainingBoost, time, z, y);
    return result;
  }

  private static void registerAerialInfo(AerialInfo info) {
    // Lint.CHANGE_IF(#getInfos)
    long boost = roundDown(info.boost, 1);
    long height = roundDown(info.height, 20);
    // End Lint.CHANGE_IF

    if (!BOOST_HEIGHT_LOOKUP.contains(boost, height)) {
      BOOST_HEIGHT_LOOKUP.put(boost, height, new ArrayList<>(50));
    }
    List<AerialInfo> infoEntry = BOOST_HEIGHT_LOOKUP.get(boost, height);

    if (infoEntry.stream().noneMatch(entry -> entry.boostAngle == info.boostAngle)) {
      infoEntry.add(info);
    }
  }

  public static List<AerialInfo> getInfos(double boost, float height) {
    // Lint.CHANGE_IF(#getInfos)
    long boostLong = roundDown(boost, 1);
    long heightLong = roundDown(height, 20);
    // End Lint.CHANGE_IF

    List<AerialInfo> entries = BOOST_HEIGHT_LOOKUP.get(boostLong, heightLong);

    return entries == null ? ImmutableList.of() : ImmutableList.copyOf(entries);
  }

  public static class AerialInfo {
    public final double boostAngle; // radians
    public final double boost;
    public final double boostUsed;
    public final double time;
    public final double nonBoostingTime;
    public final double height;
    public final double horizontalTravel;

    AerialInfo(double boostAngle, double boost, double boostUsed, double time, double height, double horizontalTravel) {
      this.boostAngle = boostAngle;
      this.boost = boost;
      this.boostUsed = boostUsed;
      this.time = time;
      this.height = height;
      this.horizontalTravel = horizontalTravel;
      this.nonBoostingTime = time - (boostUsed / Constants.BOOST_RATE);
    }
  }
}
