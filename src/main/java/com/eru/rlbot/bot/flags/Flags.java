package com.eru.rlbot.bot.flags;

import com.google.common.collect.Range;

public final class Flags {

  public static final boolean slow_time_near_ball = false;
  public static final boolean kickoff_game_enabled = false;

  public static final Range<Integer> bot_rendering_ids = Range.all();

  private Flags() {
  }
}
