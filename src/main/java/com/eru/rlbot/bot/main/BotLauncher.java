package com.eru.rlbot.bot.main;

import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.common.util.PortReader;
import rlbot.manager.BotManager;
import rlbot.pyinterop.PythonServer;

/**
 * See JavaAgent.py for usage instructions.
 */
public class BotLauncher {

  public static void main(String[] args) {
    BotManager botManager = new BotManager();
    botManager.setRefreshRate(Constants.STEP_SIZE_COUNT);
    BotFactory botFactory = new BotFactory(botManager);
    Integer port = PortReader.readPortFromFile("port.cfg");

    PythonServer pythonServer = new PythonServer(botFactory, port);
    pythonServer.start();

    AgcUi.createUi(botFactory, port);
  }
}
