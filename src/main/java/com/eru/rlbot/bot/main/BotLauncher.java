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

    panel.add(stateSettingButtons(botManager, botMaker));

    frame.add(panel);

    frame.pack();
    frame.setVisible(true);

    registerBotRunningListener(botsRunning, botManager);
  }

  private static JPanel stateSettingButtons(BotManager botManager, BotFactory factory) {
    final JPanel stateSettingPanel = new JPanel();
    stateSettingPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
    stateSettingPanel.setLayout(new BoxLayout(stateSettingPanel, BoxLayout.Y_AXIS));

    ActionListener myListener = e -> {
      stateSettingPanel.removeAll();
      for (Integer botIndex : botManager.getRunningBotIndices()) {
        Agc bot = factory.getBot(botIndex);
        if (bot == null) {
          continue;
        }

        // TODO: Disable based on flag.
        JButton stateSettingButton = new JButton(String.format("Set State: %d", System.currentTimeMillis()));
//        stateSettingButton.addActionListener((event) -> {
//          int index = Integer.valueOf(event.getActionCommand().substring(event.getActionCommand().length() - 1));
//          factory.getBot(index).enableStateSetting();
//          stateSettingButton.setEnabled(true);
//        });
        stateSettingButton.setEnabled(true);
        stateSettingPanel.add(stateSettingButton);
      }
    };

    new Timer(10000, myListener).start();

    return stateSettingPanel;
  }

  private static void registerBotRunningListener(JLabel label, BotManager botManager) {
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
      label.setText("Bots running: " + botsStr);
    };

    new Timer(1000, myListener).start();
  }
}
