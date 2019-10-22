package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.vector.Vector3;

import static com.eru.rlbot.bot.common.Constants.HALF_LENGTH;

public class Goal {

  private static final int WIDTH = 1785; // 892.755 * 2
  private static final int HEIGHT = 643; // Technically 642.775

  private static final Vector3 ORANGE_CENTER = Vector3.of(0, HALF_LENGTH, 0);
  private static final Vector3 BLUE_CENTER = Vector3.of(0, -1 * HALF_LENGTH, 0);
  private static final Vector3 HALF_GOAL = Vector3.of(WIDTH / 2f, 0, 0);

  private static final Goal BLUE_GOAL = new Goal(BLUE_CENTER);
  private static final Goal ORANGE_GOAL = new Goal(ORANGE_CENTER);

  public static Goal ownGoal(int team) {
    return team == 1 ? ORANGE_GOAL : BLUE_GOAL;
  }

  public static Goal opponentGoal(int team) {
    return team == 1 ? ORANGE_GOAL : BLUE_GOAL;
  }

  // TODO: Add cross-bar
  public final Vector3 center;
  public final Vector3 left;
  public final Vector3 right;

  private Goal(Vector3 center) {
    this.center = center;
    this.left = center.y < 0 ? center.plus(HALF_GOAL) : center.minus(HALF_GOAL);
    this.right = center.y > 0 ? center.plus(HALF_GOAL) : center.minus(HALF_GOAL);
  }
}
