package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.Pair;
import com.google.common.base.Preconditions;
import java.util.stream.IntStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rlbot.flat.GameTickPacket;
import rlbot.flat.Touch;

/**
 * Helper for team related information.
 */
public class Teams {

  private static final Logger logger = LogManager.getLogger("Teams");

  private static ThreadLocal<GameTickPacket> packet = new ThreadLocal<>();

  public static void track(GameTickPacket packet) {
    Teams.packet.set(packet);
  }

  public static Touch getBallTouchTime() {
    return packet.get().ball().latestTouch();
  }

  public static int getTeamForBot(int index) {
    Preconditions.checkState(packet.get().playersLength() > index, "No player for index " + index);
    return packet.get().players(index).team();
  }

  public static int getTeamSize(int team) {
    return (int) IntStream.range(0, packet.get().playersLength())
        .mapToObj(index -> packet.get().players(index))
        .filter(playerInfo -> playerInfo.team() == team)
        .count();
  }

  public static Pair<Integer, Integer> getScore() {
    if (packet.get().teams(0).teamIndex() != 0 || packet.get().teams(1).teamIndex() != 1) {
      logger.warn("Warning: Expectation violated.");
    }
    return Pair.of(packet.get().teams(0).score(), packet.get().teams(1).score());
  }

  public static int otherTeam(int team) {
    return (team + 1) % 2;
  }

  private Teams() {
  }
}
