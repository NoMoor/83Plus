package com.eru.rlbot.bot.common;

import rlbot.Bot;
import rlbot.cppinterop.RLBotDll;
import rlbot.flat.QuickChatSelection;
import com.eru.rlbot.common.input.DataPacket;

/** Chats stuff. */
public class BotChatter {

    private Bot bot;

    public static BotChatter forBot(Bot bot) {
        return new BotChatter(bot);
    }

    private BotChatter(Bot bot) {
        this.bot = bot;
    }

    private float lasChat;
    public void talk(DataPacket input) {
        if (input.car.elapsedSeconds - lasChat < 5) {
            return;
        }

        // This is also optional!
        if (input.ball.position.y > Goal.opponentGoal(input.team).center.y) {
            RLBotDll.sendQuickChat(bot.getIndex(), false, QuickChatSelection.Reactions_Savage);
            lasChat = input.car.elapsedSeconds;
        }
    }
}
