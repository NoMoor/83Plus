package com.eru.rlbot.bot.flags;

import com.google.common.collect.Range;

/**
 * Variables controlling all the optional bot states, logging, rendering, etc.
 */
public final class Flags {

  // TODO: Turn this into a model that can be modified using the UI.

  public static final boolean SLOW_TIME_NEAR_BALL_ENABLED = false;
  public static final boolean ENABLE_KICKOFF_GAME = false;
  public static final boolean ENABLE_TRAIL_RENDERING = false;

  public static final boolean FREEZE_CAR_ENABLED = false;
  public static final boolean PREDICT_AND_RENDER_NEXT_CAR_FRAME_ENABLED = false;

  public static final Range<Integer> BOT_RENDERING_IDS = Range.singleton(0);
  public static final boolean STATE_LOGGER_ENABLED = false;

  private Flags() {
  }
}
