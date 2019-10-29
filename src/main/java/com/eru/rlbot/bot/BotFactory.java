package com.eru.rlbot.bot;

import com.eru.rlbot.bot.ballchaser.v0.BallChaserV0;
import com.eru.rlbot.bot.ballchaser.v1.BallChaserV1;
import rlbot.Bot;
import rlbot.manager.BotManager;
import rlbot.pyinterop.DefaultPythonInterface;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BotFactory extends DefaultPythonInterface {

  private static final String UNKNOWN_VERSION = "unknown";

  public BotFactory(BotManager botManager) {
    super(botManager);
  }

  protected Bot initBot(int index, String botType, int team) {
    String version = getVersion(botType);

    switch (version) {
      case "0":
        return new BallChaserV0(index);
      case "1":
      default:
        return new BallChaserV1(index, team);
    }
  }

  private String getVersion(String botType) {
    Pattern versionPattern = Pattern.compile("v[0-9]\\.?[0-9]?");
    Matcher matcher = versionPattern.matcher(botType);

    if (!matcher.find()) {
      return UNKNOWN_VERSION;
    }

    return botType.substring(matcher.start() + 1, matcher.end());
  }
}
