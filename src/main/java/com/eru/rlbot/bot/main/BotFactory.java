package com.eru.rlbot.bot.main;

import rlbot.Bot;
import rlbot.manager.BotManager;
import rlbot.pyinterop.DefaultPythonInterface;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Factor for creating {@link EruBot}s. */
public final class BotFactory extends DefaultPythonInterface {

  BotFactory(BotManager botManager) {
    super(botManager);
  }

  protected Bot initBot(int playerIndex, String botType, int team) {
    return new EruBot(playerIndex, team);
  }
}
