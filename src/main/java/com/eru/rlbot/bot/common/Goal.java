package com.eru.rlbot.bot.common;

import static com.eru.rlbot.bot.common.Constants.BALL_RADIUS;
import static com.eru.rlbot.bot.common.Constants.HALF_LENGTH;

import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;

/**
 * Class holding various goal constants.
 */
public final class Goal {

  private static final Vector3 ORANGE_CENTER = Vector3.of(0, HALF_LENGTH, 0);
  private static final Vector3 BLUE_CENTER = Vector3.of(0, -1 * HALF_LENGTH, 0);
  private static final Vector3 HALF_GOAL = Vector3.of(Constants.GOAL_WIDTH / 2f, 0, 0);
  private static final Vector3 HALF_GOAL_INSIDE = Vector3.of((Constants.GOAL_WIDTH / 2f) - 2 * BALL_RADIUS, 0, 0);
  private static final Vector3 HALF_GOAL_OUTSIDE = Vector3.of((Constants.GOAL_WIDTH / 2f) + 2 * BALL_RADIUS, 0, 0);

  private static final Goal BLUE_GOAL = new Goal(BLUE_CENTER);
  private static final Goal ORANGE_GOAL = new Goal(ORANGE_CENTER);

  /**
   * Returns the goal of the given team.
   */
  public static Goal ownGoal(int team) {
    return team == 1 ? ORANGE_GOAL : BLUE_GOAL;
  }

  public static Goal ownGoal(DataPacket input) {
    return ownGoal(input.car.team);
  }

  /**
   * Returns the opponent's goal.
   */
  public static Goal opponentGoal(int team) {
    return team == 0 ? ORANGE_GOAL : BLUE_GOAL;
  }

  public static Goal opponentGoal(DataPacket input) {
    return opponentGoal(input.car.team);
  }

  public final Vector3 center;
  public final Vector3 centerTop;
  public final Vector3 left;
  public final Vector3 right;
  public final Vector3 leftInside;
  public final Vector3 rightInside;
  public final Vector3 leftWide;
  public final Vector3 rightWide;
  public final Vector3 leftUpperCorner;
  public final Vector3 rightUpperCorner;

  private Goal(Vector3 center) {
    this.center = center;
    this.centerTop = center.addZ(Constants.GOAL_HEIGH - BALL_RADIUS);
    this.left = center.y > 0 ? center.plus(HALF_GOAL) : center.minus(HALF_GOAL);
    this.leftInside = center.y > 0 ? center.plus(HALF_GOAL_INSIDE) : center.minus(HALF_GOAL_INSIDE);
    this.leftWide = center.y > 0 ? center.plus(HALF_GOAL_OUTSIDE) : center.minus(HALF_GOAL_OUTSIDE);
    this.leftUpperCorner = left.addZ(Constants.GOAL_HEIGH - BALL_RADIUS);
    this.right = center.y < 0 ? center.plus(HALF_GOAL) : center.minus(HALF_GOAL);
    this.rightInside = center.y < 0 ? center.plus(HALF_GOAL_INSIDE) : center.minus(HALF_GOAL_INSIDE);
    this.rightWide = center.y < 0 ? center.plus(HALF_GOAL_OUTSIDE) : center.minus(HALF_GOAL_OUTSIDE);
    this.rightUpperCorner = right.addZ(Constants.GOAL_HEIGH - BALL_RADIUS);
  }

  /**
   * Returns the post on the same side as the given car.
   */
  public Vector3 getNearPost(CarData car) {
    return getNearPost(car.position);
  }

  public Vector3 getNearPost(Vector3 location) {
    return Math.signum(location.x) == Math.signum(right.x) ? right : left;
  }

  public Vector3 getFarPost(Vector3 supportLocation) {
    return Math.signum(supportLocation.x) == -Math.signum(right.x) ? right : left;
  }
}
