package com.eru.rlbot.bot.chat;

import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.common.input.DataPacket;
import java.util.concurrent.ConcurrentHashMap;
import rlbot.Bot;
import rlbot.cppinterop.RLBotDll;
import rlbot.flat.QuickChatSelection;

/**
 * Sends radio messages to other bots.
 */
public class RadioModule {

  private static ConcurrentHashMap<Integer, RadioModule> MAP = new ConcurrentHashMap<>();

  private int chatNumber;
  private float lastChat;

  /** Creates and returns a module for the given bot. */
  public static RadioModule create(Bot bot) {
    return MAP.computeIfAbsent(bot.getIndex(), index -> new RadioModule(bot));
  }

  private RadioModule(Bot bot) {
    this.chatNumber = bot.getIndex();
  }

  /** Sends messages for fun. */
  public void sendMessages(DataPacket input) {
    if (input.car.elapsedSeconds - lastChat < 1) {
      return;
    }

    if (input.allCars.stream().anyMatch(car -> car.isDemolished)) {
      RLBotDll.sendQuickChat(chatNumber, false, QuickChatSelection.Reactions_Savage);
      lastChat = input.car.elapsedSeconds;
    }

    float oppGoalY = Goal.opponentGoal(input.alliance).center.y;
    float ballY = input.ball.position.y;
    if (Math.signum(oppGoalY) == Math.signum(ballY) && Math.abs(ballY) > Math.abs(oppGoalY)) {
      RLBotDll.sendQuickChat(chatNumber, false, QuickChatSelection.Compliments_NiceShot);
      lastChat = input.car.elapsedSeconds;
    }
  }
}
