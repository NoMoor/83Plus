package com.eru.rlbot.bot.maneuver;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.common.Matrix3;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.output.Controls;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WallHelper {

  private static final Logger logger = LogManager.getLogger("WallHelper");

  private static final float BACK_WALL_X = 2600;
  private static final float BACK_WALL_Y = 5100;
  private static final float CORNER_WALL_X = 3480;
  private static final float CORNER_WALL_Y = 4560;
  private static final float SIDE_WALL_X = 4074;
  private static final float SIDE_WALL_Y = 3650;

  private static final float LENGTH_WIDTH_DIFFERENTIAL = CORNER_WALL_Y - CORNER_WALL_X; // ~1080UU

  private static final float PADDING = 10;

  public static boolean isOnWall(CarData car) {
    return car.hasWheelContact && car.position.z > 30;
  }

  public static void drive(DataPacket input, Controls output, Vector3 targetPosition) {
    CarData car = input.car;

    Vector2 projectedLocation = getProjectedLocation(car);
    Vector2 projectedOrientation = getProjectedOrientation(car);

    double correctionAngle = Angles.flatCorrectionAngle(projectedLocation, projectedOrientation, targetPosition.flatten());
    output
        .withSteer(correctionAngle)
        .withThrottle(1.0f)
        .withBoost(Math.abs(correctionAngle) < .3 && input.car.velocity.magnitude() < 1600 && input.car.boost > 50);

    if (isNearGoal(input)) {
      // Jump off the wall near the goal.
      output.withJump();
    }
  }

  private static boolean isNearGoal(DataPacket input) {
    boolean nearGoalEdge = Math.abs(input.car.position.x) < ((Constants.GOAL_WIDTH / 2f) + 100);
    boolean nearGoalHeight = input.car.position.y < Constants.GOAL_HEIGH + 50;
    boolean isMovingTowardGoal = Math.signum(input.car.velocity.x) != Math.signum(input.car.position.x) ||
        input.car.velocity.y < 0;

    return nearGoalEdge && nearGoalHeight && isMovingTowardGoal;
  }

  public static Vector2 getProjectedOrientation(CarData car) {
    if (!isOnWall(car)) {
      return car.orientation.getNoseVector().flatten();
    }

    Vector3 pos = car.position;
    boolean isLeftWall = pos.x > 0;
    boolean isFrontWall = pos.y > 0;

    if (Math.abs(pos.x) > SIDE_WALL_X - PADDING) {
      return sideWallOrientation(car, isLeftWall);
    } else if (Math.abs(pos.y) > BACK_WALL_Y - PADDING) {
      return backWallOrientation(car, isFrontWall);
    } else {
      boolean useBackWallProjection = Math.abs(pos.y) - Math.abs(pos.x) - LENGTH_WIDTH_DIFFERENTIAL > 0;
      return useBackWallProjection ? backWallOrientation(car, isFrontWall) : sideWallOrientation(car, isLeftWall);
    }
  }

  private static Orientation LEFT_WALL_ORIENTATION = Orientation.convert(-1, 0, 0);
  private static Orientation FRONT_WALL_ORIENTATION = Orientation.convert(0, 0, 1);

  private static Vector2 backWallOrientation(CarData car, boolean isFrontWall) {
    Matrix3 transform = isFrontWall
        ? FRONT_WALL_ORIENTATION.getOrientationMatrix() : FRONT_WALL_ORIENTATION.getOrientationMatrix().transpose();
    return transform.rotateOrientation(car.orientation.getOrientationMatrix()).column(0).flatten();
  }

  private static Vector2 sideWallOrientation(CarData car, boolean isLeftWall) {
    Matrix3 transform = isLeftWall
        ? LEFT_WALL_ORIENTATION.getOrientationMatrix() : LEFT_WALL_ORIENTATION.getOrientationMatrix().transpose();
    return transform.rotateOrientation(car.orientation.getOrientationMatrix()).column(0).flatten();
  }

  public static Vector2 getProjectedLocation(CarData car) {
    if (!isOnWall(car)) {
      return car.position.flatten();
    }

    Vector3 pos = car.position;
    boolean isLeftWall = pos.x > 0;
    boolean isFrontWall = pos.y > 0;

    if (Math.abs(pos.x) > SIDE_WALL_X - PADDING) {
      return sideWallProjection(car.position, isLeftWall);
    } else if (Math.abs(pos.y) > BACK_WALL_Y - PADDING) {
      return backWallProjection(car.position, isFrontWall);
    } else {
      boolean useBackWallProjection = Math.abs(pos.y) - Math.abs(pos.x) - LENGTH_WIDTH_DIFFERENTIAL > 0;
      return useBackWallProjection ? backWallProjection(pos, isFrontWall) : sideWallProjection(pos, isLeftWall);
    }
  }

  // TODO: These radii are different
  private static Vector2 backWallProjection(Vector3 position, boolean isFrontWall) {
    float projectionModifier = isFrontWall ? 1 : -1;
    return position.addY(projectionModifier * Math.max(position.z - 10, 0)).flatten();
  }

  private static Vector2 sideWallProjection(Vector3 position, boolean isLeftWall) {
    float projectionModifier = isLeftWall ? 1 : -1;
    return position.addX(projectionModifier * Math.max(position.z - 50, 0)).flatten();
  }
}
