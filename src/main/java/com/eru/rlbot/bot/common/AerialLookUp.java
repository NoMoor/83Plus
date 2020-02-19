package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class AerialLookUp {

  // Height rounded to nearest 50
  private static TreeMap<Long, List<AerialInfo>> APEX_MAP = new TreeMap<>();
  private static final double FAST_AERIAL_VELOCITY = 717.0;

  static {
    for (double r = .75; r <= Math.PI / 2; r += .1) {
      for (int i = 0; i <= 100; i++) {
        AerialInfo result = computeAerialResult(r, i);

        APEX_MAP.computeIfAbsent(toKey(result.apexHeight), (key) -> new ArrayList<>()).add(result);
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
    return Math.round(height / 50.0) * 50;
  }

  private static AerialInfo computeAerialResult(final double boostAngle, final double initialBoostAmount) {
    double time = 0;
    double y = 0;
    double z = 0;
    double remainingBoost = initialBoostAmount;

    Vector3 velocity = Vector3.of(0, 0, FAST_AERIAL_VELOCITY);
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
    }

    // Coast to max height
    while (velocity.z > 0) {
      time += Constants.STEP_SIZE;
      velocity = velocity.plus(gravity);
      y += velocity.y * Constants.STEP_SIZE;
      z += velocity.z * Constants.STEP_SIZE;
    }

    return new AerialInfo(boostAngle, initialBoostAmount, time, z, y);
  }

  public static class AerialInfo {
    public final double boostAngle; // radians
    public final double boostAmount;
    public final double timeToApex;
    public final double nonBoostingTime;
    public final double apexHeight;
    public final double horizontalTravel;

    AerialInfo(double boostAngle, double boostAmount, double timeToApex, double apexHeight, double horizontalTravel) {
      this.boostAngle = boostAngle;
      this.boostAmount = boostAmount;
      this.timeToApex = timeToApex;
      this.apexHeight = apexHeight;
      this.horizontalTravel = horizontalTravel;
      this.nonBoostingTime = timeToApex - (boostAmount * Constants.BOOST_RATE);
    }
  }
}
