package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.vector.Vector3;

import static com.eru.rlbot.bot.common.Constants.FIELD_SIZE;

public class Goal {

  private static final Vector3 ORANGE_CENTER = new Vector3(0, -1 * FIELD_SIZE, 0);
  private static final Vector3 BLUE_CENTER = new Vector3(0, FIELD_SIZE, 0);

  private static final Goal BLUE_GOAL = new Goal(BLUE_CENTER);
  private static final Goal ORANGE_GOAL = new Goal(ORANGE_CENTER);

  private static final int WIDTH = 3000;
  private static final int HEIGHT = 1500;

  public static Goal ownGoal(int team) {
    return team == 1 ? ORANGE_GOAL : BLUE_GOAL;
  }

  public static Goal opponentGoal(int team) {
    return team == 1 ? ORANGE_GOAL : BLUE_GOAL;
  }

  public final Vector3 center;

  private Goal(Vector3 center) {
    this.center = center;
  }
}
