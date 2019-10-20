package com.eru.rlbot.bot;

import com.eru.rlbot.bot.common.BotChatter;
import com.eru.rlbot.bot.common.BotRenderer;
import com.eru.rlbot.bot.common.Goal;
import rlbot.Bot;

import static com.eru.rlbot.bot.common.Goal.opponentGoal;
import static com.eru.rlbot.bot.common.Goal.ownGoal;

public abstract class EruBot implements Bot {

  public final Goal opponentsGoal;
  public final Goal ownGoal;
  public final int team;
  public final BotRenderer botRenderer;
  protected final int playerIndex;
  protected final BotChatter botChatter;

  public EruBot(int playerIndex, int team) {
    this.playerIndex = playerIndex;
    this.team = team;

    this.botRenderer = BotRenderer.forBot(this);
    this.botChatter = BotChatter.forBot(this);

    opponentsGoal = opponentGoal(team);
    ownGoal = ownGoal(team);
  }
}
