package com.eru.rlbot.bot.main;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import rlbot.manager.BotManager;
import rlbot.pyinterop.DefaultPythonInterface;

/**
 * Factor for creating {@link Agc}s.
 */
public final class BotFactory extends DefaultPythonInterface {

  private ConcurrentHashMap<Integer, Agc> bots = new ConcurrentHashMap<>();
  protected final BotManager botManager;

  BotFactory(BotManager botManager) {
    super(botManager);
    this.botManager = botManager;
  }

  protected Agc initBot(int playerIndex, String botName, int team) {
    return bots.computeIfAbsent(playerIndex, (index) -> new Agc(playerIndex, botName, team));
  }

  public Agc getBot(int index) {
    return bots.get(index);
  }

  public List<Agc> getBots() {
    return ImmutableList.copyOf(bots.values());
  }
}
