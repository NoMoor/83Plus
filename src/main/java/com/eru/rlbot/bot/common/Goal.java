package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.vector.Vector3;

import static com.eru.rlbot.bot.common.Constants.HALF_LENGTH;

public class Goal {

  private static final Vector3 ORANGE_CENTER = new Vector3(0, HALF_LENGTH, 0);
  private static final Vector3 BLUE_CENTER = new Vector3(0, -1 * HALF_LENGTH, 0);

  private static final Goal BLUE_GOAL = new Goal(BLUE_CENTER);
  private static final Goal ORANGE_GOAL = new Goal(ORANGE_CENTER);

  private static final int WIDTH = 1785; // 892.755 * 2
  private static final int HEIGHT = 643; // Technically 642.775

  public static Goal ownGoal(int team) {
    return team == 1 ? ORANGE_GOAL : BLUE_GOAL;
  }

  public static Goal opponentGoal(int team) {
    return team == 1 ? ORANGE_GOAL : BLUE_GOAL;
  }

  // TODO: Add left post, right post, cross-bar
  public final Vector3 center;

  private Goal(Vector3 center) {
    this.center = center;
  }
}
