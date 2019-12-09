package com.eru.rlbot.bot.main;

import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.common.util.PortReader;
import rlbot.manager.BotManager;
import rlbot.pyinterop.PythonServer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.ActionListener;
import java.util.OptionalInt;
import java.util.Set;

/**
 * See JavaAgent.py for usage instructions.
 */
public class BotLauncher {

  public static void main(String[] args) {

    BotManager botManager = new BotManager();
    botManager.setRefreshRate(Constants.STEP_SIZE_COUNT);
    BotFactory botMaker = new BotFactory(botManager);
    Integer port = PortReader.readPortFromFile("port.cfg");

    PythonServer pythonServer = new PythonServer(botMaker, port);
    pythonServer.start();

    JFrame frame = new JFrame("Java Bot");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    JPanel panel = new JPanel();
    panel.setBorder(new EmptyBorder(10, 10, 10, 10));
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.add(new JLabel("Listening on port " + port));
    panel.add(new JLabel("I'm the thing controlling the Java bot, keep me open :)"));
    JLabel botsRunning = new JLabel("Bots running: ");
    panel.add(botsRunning);
    frame.add(panel);

    frame.pack();
    frame.setVisible(true);

    ActionListener myListener = e -> {
      Set<Integer> runningBotIndices = botManager.getRunningBotIndices();
      OptionalInt maxIndex = runningBotIndices.stream().mapToInt(k -> k).max();
      String botsStr = "None";
      if (maxIndex.isPresent()) {
        StringBuilder botsStrBuilder = new StringBuilder();
        for (int i = 0; i <= maxIndex.getAsInt(); i++) {
          botsStrBuilder.append(runningBotIndices.contains(i) ? "☑ " : "☐ ");
        }
        botsStr = botsStrBuilder.toString();
      }
      botsRunning.setText("Bots running: " + botsStr);
    };

    new Timer(1000, myListener).start();
  }
}
