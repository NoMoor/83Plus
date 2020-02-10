package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.input.DataPacket;
import rlbot.Bot;
import rlbot.cppinterop.RLBotDll;
import rlbot.flat.QuickChatSelection;

/** Chats stuff. */
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
        if (input.ball.position.y > Goal.opponentGoal(input.team).center.y) {
            RLBotDll.sendQuickChat(bot.getIndex(), false, QuickChatSelection.Reactions_Savage);
            lasChat = input.car.elapsedSeconds;
        }
    }
}
