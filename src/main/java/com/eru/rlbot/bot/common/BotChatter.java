package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.input.DataPacket;
import rlbot.Bot;
import rlbot.cppinterop.RLBotDll;
import rlbot.flat.QuickChatSelection;

/**
 * Chats stuff.
 */
public class BotChatter {

  private Bot bot;

  public static BotChatter forBot(Bot bot) {
    return new BotChatter(bot);
  }

  private BotChatter(Bot bot) {
    this.bot = bot;
  }

  private int chatNumber = 0;
  private float lasChat;

  public void talk(DataPacket input) {
    if (input.car.elapsedSeconds - lasChat < 1) {
      return;
    }

    // This is also optional!
    float oppGoalY = Goal.opponentGoal(input.team).center.y;
    float ballY = input.ball.position.y;
    if (Math.signum(oppGoalY) == Math.signum(ballY) && Math.abs(ballY) > Math.abs(oppGoalY)) {
      RLBotDll.sendQuickChat(bot.getIndex(), false, QuickChatSelection.Compliments_NiceShot);
      lasChat = input.car.elapsedSeconds;
    }
  }
}
