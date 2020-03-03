package com.eru.rlbot.bot.strats;

import com.eru.rlbot.bot.main.ApolloGuidanceComputer;

/**
 * List of high level strategies.
 */
class Strategy {

  /** Types of strategies. */
  enum Type {
    ATTACK,
    DEFEND,
    SUPPORT
  }

  /** Gets a strategist of the given type for a bot. */
  static Strategist strategistForBot(Strategy.Type type, ApolloGuidanceComputer bot) {
    switch (type) {
      case ATTACK:
        return new AttackStrategist(bot);
      case DEFEND:
        return new DefendStrategist(bot);
      case SUPPORT:
        return new SupportStrategist(bot);
    }

    throw new IllegalArgumentException(String.format("Unknown strategy tacticType %s", type));
  }
}
