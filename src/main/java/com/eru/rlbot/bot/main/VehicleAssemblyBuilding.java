package com.eru.rlbot.bot.main;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import rlbot.manager.BotManager;
import rlbot.pyinterop.SocketServer;

/**
 * Rockets don't just put themselves together. They require hard work.
 */
public final class VehicleAssemblyBuilding extends SocketServer {

  /**
   * A collection of rockets, organized by serial number.
   */
  private final ConcurrentHashMap<Integer, ApolloGuidanceComputer> rocketCatalog = new ConcurrentHashMap<>();

  protected final BotManager botManager;

  VehicleAssemblyBuilding(int commChannel, BotManager botManager) {
    super(commChannel, botManager);
    this.botManager = botManager;
  }

  protected ApolloGuidanceComputer initBot(int playerIndex, String botName, int team) {
    return rocketCatalog.computeIfAbsent(playerIndex, (index) -> new ApolloGuidanceComputer(playerIndex, botName, team));
  }

  /**
   * Gets the bot with the given index.
   */
  public ApolloGuidanceComputer getBot(int serialNumber) {
    return rocketCatalog.get(serialNumber);
  }

  /**
   * Gets the list of all the created rockets.
   */
  public List<ApolloGuidanceComputer> getRocketList() {
    return ImmutableList.copyOf(rocketCatalog.values());
  }
}
