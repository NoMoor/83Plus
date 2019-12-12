package com.eru.rlbot.bot.main;

import rlbot.manager.BotManager;
import rlbot.pyinterop.DefaultPythonInterface;
import java.util.HashMap;
import java.util.Map;

/**
 * Factor for creating {@link Agc}s.
 */
public final class BotFactory extends DefaultPythonInterface {

  private Map<Integer, Agc> bots = new HashMap<>();

  BotFactory(BotManager botManager) {
    super(botManager);
  }

  protected Agc initBot(int playerIndex, String botType, int team) {
    return bots.computeIfAbsent(playerIndex, (index) -> new Agc(playerIndex, team));
  }

  public Agc getBot(int index) {
    return bots.get(index);
  }
}
