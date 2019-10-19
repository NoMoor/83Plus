package com.eru.rlbot.bot.ballchaser.v1.strats;

import com.eru.rlbot.bot.EruBot;

class Strategy {

  enum Type {
    ATTACK,
    DEFEND,
    SUPPORT

//    /** Shoot straight at the goal. */
//    TAKE_SHOT,
//    /** Center the ball for someone else to shoot. */
//    INFIELD_PASS,
//    /** Pass back to a teammate. */
//    BACK_PASS,
//    /** Move away from what you are doing. */
//    DISENGAGE,
//    /** Follow team as they attack. */
//    SUPPORT,
//    /** Bouncing a shot off the opponents back wall. */
//    BACKBOARD,
//    /** Defending as last man back. Stall for time. */
//    SHADOW,
//    /** Solo dribbling. */
//    DRIBBLE
  }

  static Strategist strategistForBot(Strategy.Type type, EruBot bot) {
    switch (type) {
      case ATTACK:
        return new AttackStrategist(bot);
      case DEFEND:
        return new DefendStategist(bot);
      case SUPPORT:
        return new SupportStategist(bot);
    }

    throw new IllegalArgumentException(String.format("Unknown strategy type %s", type));
  }
}
