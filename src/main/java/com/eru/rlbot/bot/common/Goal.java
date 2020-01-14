package com.eru.rlbot.bot.common;

import static com.eru.rlbot.bot.common.Constants.BALL_RADIUS;
import static com.eru.rlbot.bot.common.Constants.HALF_LENGTH;

import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.vector.Vector3;

public class Goal {

  private static final Vector3 ORANGE_CENTER = Vector3.of(0, HALF_LENGTH, 0);
  private static final Vector3 BLUE_CENTER = Vector3.of(0, -1 * HALF_LENGTH, 0);
  private static final Vector3 HALF_GOAL = Vector3.of(Constants.GOAL_WIDTH / 2f, 0, 0);
  private static final Vector3 HALF_GOAL_INSIDE = Vector3.of((Constants.GOAL_WIDTH / 2f) - BALL_RADIUS, 0, 0);
  private static final Vector3 HALF_GOAL_OUTSIDE = Vector3.of((Constants.GOAL_WIDTH / 2f) + BALL_RADIUS, 0, 0);

  private static final Goal BLUE_GOAL = new Goal(BLUE_CENTER);
  private static final Goal ORANGE_GOAL = new Goal(ORANGE_CENTER);

  public static Goal ownGoal(int team) {
    return team == 1 ? ORANGE_GOAL : BLUE_GOAL;
  }

  public static Goal opponentGoal(int team) {
    return team == 0 ? ORANGE_GOAL : BLUE_GOAL;
  }

  // TODO: Add cross-bar
  public final Vector3 center;
  public final Vector3 centerTop;
  public final Vector3 left;
  public final Vector3 right;
  public final Vector3 leftInside;
  public final Vector3 rightInside;
  public final Vector3 leftOutside;
  public final Vector3 rightOutside;

  private Goal(Vector3 center) {
    this.center = center;
    this.centerTop = center.addZ(Constants.GOAL_HEIGH - BALL_RADIUS);
    this.left = center.y > 0 ? center.plus(HALF_GOAL) : center.minus(HALF_GOAL);
    this.leftInside = center.y > 0 ? center.plus(HALF_GOAL_INSIDE) : center.minus(HALF_GOAL_INSIDE);
    this.leftOutside = center.y > 0 ? center.plus(HALF_GOAL_OUTSIDE) : center.minus(HALF_GOAL_OUTSIDE);
    this.right = center.y < 0 ? center.plus(HALF_GOAL) : center.minus(HALF_GOAL);
    this.rightInside = center.y < 0 ? center.plus(HALF_GOAL_INSIDE) : center.minus(HALF_GOAL_INSIDE);
    this.rightOutside = center.y < 0 ? center.plus(HALF_GOAL_OUTSIDE) : center.minus(HALF_GOAL_OUTSIDE);
  }

  public Vector3 getSameSidePost(CarData car) {
    return Math.signum(car.position.x) == Math.signum(right.x) ? right : left;
  }
}
