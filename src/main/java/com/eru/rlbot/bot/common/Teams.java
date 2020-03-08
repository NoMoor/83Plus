package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.Pair;
import com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rlbot.flat.GameTickPacket;
import rlbot.flat.Touch;

/**
 * Helper for team related information.
 */
public class Teams {

  private static final Logger logger = LogManager.getLogger("Teams");

  private static volatile GameTickPacket packet;

  public static void track(GameTickPacket packet) {
    Teams.packet = packet;
  }

  public static Touch getBallTouchTime() {
    return packet.ball().latestTouch();
  }

  public static int getTeamForBot(int index) {
    Preconditions.checkState(packet.playersLength() > index, "No player for index " + index);
    return packet.players(index).team();
  }

  public static Pair<Integer, Integer> getScore() {
    if (packet.teams(0).teamIndex() != 0 || packet.teams(1).teamIndex() != 1) {
      logger.warn("Warning: Expectation violated.");
    }
    return Pair.of(packet.teams(0).score(), packet.teams(1).score());
  }

  private Teams() {
  }
}
