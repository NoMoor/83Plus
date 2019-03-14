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

    public void talk(DataPacket input) {
        // This is also optional!
        if (input.ball.position.z > 300) {
            RLBotDll.sendQuickChat(bot.getIndex(), false, QuickChatSelection.Compliments_NiceOne);
        }
    }
}
