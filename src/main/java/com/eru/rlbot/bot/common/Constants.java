package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.vector.Vector3;

/** RL Constants. */
public class Constants {

  // Reference: https://github.com/RLBot/RLBot/wiki/Useful-Game-Values

  public static final int HALF_WIDTH = 4096;
  public static final int HALF_LENGTH = 5120;
  public static final int FIELD_LENGTH = 2 * HALF_LENGTH;
  public static final int FIELD_WIDTH = 2 * HALF_WIDTH;
  public static final int FIELD_HEIGHT = 3000;

  public static final Vector3 LEFT_SIDE_WALL = new Vector3(HALF_WIDTH, 0, 1000);
  public static final Vector3 RIGHT_SIDE_WALL = new Vector3(-1 * HALF_WIDTH, 0, 1000);

  // TODO(ahatfield): Determine what this rae.
  public static final double BOOSTED_ACCELERATION = 200;
  public static final double ACCELERATION = 600;

  public static final float BALL_HEIGHT = 76.0f;
}
