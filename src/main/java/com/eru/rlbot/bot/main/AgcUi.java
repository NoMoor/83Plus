package com.eru.rlbot.bot.main;

import static java.awt.Component.CENTER_ALIGNMENT;

import com.eru.rlbot.bot.common.Pair;
import com.eru.rlbot.bot.flags.GlobalDebugOptions;
import com.eru.rlbot.bot.flags.PerBotDebugOptions;
import com.google.common.base.Preconditions;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

public class AgcUi {

  private static final Color APOLLO_RED = new Color(200, 15, 15);

  private final BotFactory botFactory;
  private final Integer port;
  private final JFrame frame;
  private JPanel botPanel;
  private JPanel optionsPanel;
  private JTable botTable;

  public static final void createUi(BotFactory botFactory, Integer port) {
    new AgcUi(botFactory, port).buildUi();
  }

  AgcUi(BotFactory botFactory, Integer port) {

    this.botFactory = botFactory;
    this.port = port;
    this.frame = new JFrame("AGC");
    this.botPanel = new JPanel();
  }

  private void buildUi() {
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setResizable(false);

    JPanel panel = new JPanel();
    panel.setBackground(Color.WHITE);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    // Title Panel
    JPanel agcTitlePanel = new JPanel();
    agcTitlePanel.setOpaque(false);
    agcTitlePanel.setLayout(new BoxLayout(agcTitlePanel, BoxLayout.Y_AXIS));
    agcTitlePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    JLabel agcLabel = new JLabel("Apollo Guidance Computer", SwingConstants.CENTER);
    agcLabel.setForeground(APOLLO_RED);

    Font font = Font.decode("Helvetica BOLD 36");
    agcLabel.setFont(font);

    // Status Panel
    JLabel portLabel = new JLabel("Status: Go on port " + port, SwingConstants.CENTER);
    portLabel.setFont(Font.decode("Helvetica BOLD 18"));

    agcTitlePanel.add(agcLabel);
    agcTitlePanel.add(portLabel);
    agcTitlePanel.setAlignmentX(CENTER_ALIGNMENT);
    panel.add(agcTitlePanel);
    panel.add(new JSeparator());

    JPanel settingsPanel = new JPanel();
    settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
    settingsPanel.setOpaque(false);
    settingsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    settingsPanel.add(initGlobalDebugPanel());

    botPanel = new JPanel();
    initBotPanel();
    settingsPanel.add(botPanel);

    panel.add(settingsPanel);
    frame.add(panel);

    frame.pack();
    frame.setVisible(true);
  }

  private JPanel initGlobalDebugPanel() {
    JPanel globalPanel = new JPanel();
    globalPanel.setOpaque(false);
    globalPanel.setLayout(new GridBagLayout());

    JPanel p1 = new JPanel();
    p1.setLayout(new BoxLayout(p1, BoxLayout.Y_AXIS));
    JCheckBox kickoffGameEnabled = new JCheckBox("Kickoff game", GlobalDebugOptions.isKickoffGameEnabled());
    kickoffGameEnabled.addActionListener((e) ->
        GlobalDebugOptions.setKickoffGameEnabled(kickoffGameEnabled.getModel().isSelected()));
    kickoffGameEnabled.setOpaque(false);
    p1.add(kickoffGameEnabled);

    JCheckBox stateLoggerEnabled = new JCheckBox("State logger", GlobalDebugOptions.isStateLoggerEnabled());
    kickoffGameEnabled.addActionListener((e) ->
        GlobalDebugOptions.setStateLoggerEnabled(stateLoggerEnabled.getModel().isSelected()));
    stateLoggerEnabled.setBackground(Color.WHITE);
    p1.setOpaque(false);
    p1.add(stateLoggerEnabled);

    JPanel p2 = new JPanel();
    p2.setLayout(new BoxLayout(p2, BoxLayout.Y_AXIS));
    JCheckBox slowTimeNearBallEnabled = new JCheckBox("Slow game near ball", GlobalDebugOptions.isSlowTimeNearBallEnabled());
    slowTimeNearBallEnabled.setOpaque(false);
    p2.add(slowTimeNearBallEnabled);

    JSlider gameSpeed = new JSlider(SwingConstants.HORIZONTAL, 1, 10, GlobalDebugOptions.getSlowedGameSpeed());
    gameSpeed.setEnabled(false);

    slowTimeNearBallEnabled.addActionListener((e) -> {
      GlobalDebugOptions.setSlowTimeNearBallEnabled(slowTimeNearBallEnabled.getModel().isSelected());
      gameSpeed.setEnabled(slowTimeNearBallEnabled.getModel().isSelected());
    });
    gameSpeed.addChangeListener((e) ->
        GlobalDebugOptions.setSlowedGameSpeed(gameSpeed.getModel().getValue()));

    Hashtable<Integer, JLabel> labels = new Hashtable<>();
    labels.put(1, new JLabel("" + 0.1));
    labels.put(5, new JLabel("" + 0.5));
    labels.put(10, new JLabel("" + 1.0));

    gameSpeed.setLabelTable(labels);
    gameSpeed.setMinorTickSpacing(1);
    gameSpeed.setMajorTickSpacing(5);
    gameSpeed.setPaintLabels(true);
    gameSpeed.setPaintTicks(true);
    gameSpeed.setOpaque(false);
    p2.add(gameSpeed);
    p2.setOpaque(false);

    GridBagConstraints c = new GridBagConstraints();
    c.weightx = .5;
    globalPanel.add(p1, c);
    globalPanel.add(p2, c);
    return globalPanel;
  }

  private void initBotPanel() {
    botPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    botPanel.setLayout(new BoxLayout(botPanel, BoxLayout.X_AXIS));
    botPanel.setOpaque(false);

    final List<Pair<Integer, Integer>> runningBots = new ArrayList<>();

    ActionListener myListener = e -> {
      // Only refresh the panel when the number of bots has changed.
      List<Pair<Integer, Integer>> currentRunningBots = botFactory.getBots().stream()
          .filter(bot -> botFactory.botManager.getRunningBotIndices().contains(bot.playerIndex))
          .map(bot -> Pair.of(bot.playerIndex, bot.team))
          .sorted(Comparator.comparing(Pair::getFirst))
          .collect(Collectors.toList());

      if (runningBots.equals(currentRunningBots)) {
        return;
      } else {
        runningBots.clear();
        runningBots.addAll(currentRunningBots);
      }

      // Clear the panel
      botPanel.removeAll();

      // Create a table
      botTable = new JTable();
      botTable.getTableHeader().setReorderingAllowed(false);
      DefaultTableModel tableModel = new DefaultTableModel();

      tableModel.addColumn("Name");
      tableModel.addColumn("Index");
      tableModel.addColumn("Team");

      JScrollPane botTableScrollPane = new JScrollPane(botTable);
      botTableScrollPane.setPreferredSize(new Dimension(200, 100));
      botTableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      botTableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

      botPanel.add(botTableScrollPane);

      for (int index : botFactory.botManager.getRunningBotIndices()) {
        Agc bot = botFactory.getBot(index);
        if (bot != null) {
          tableModel.addRow(new String[]{bot.getName(), String.valueOf(bot.getIndex()), bot.team == 0 ? "Blue" : "Orange"});
        }
      }

      botTable.setAutoCreateRowSorter(true);
      botTable.setModel(tableModel);
      botTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

      botTable.getSelectionModel().addListSelectionListener(handleRowSelection(botTable));
      botTable.setOpaque(false);
      botTableScrollPane.setOpaque(false);

      botPanel.revalidate();
      botPanel.repaint();
      frame.pack();
    };

    new Timer(1000, myListener).start();
  }

  private ListSelectionListener handleRowSelection(JTable table) {
    return (selectionEvent) -> {
      if (selectionEvent.getValueIsAdjusting())
        return;

      DefaultListSelectionModel selectionModel = (DefaultListSelectionModel) selectionEvent.getSource();
      int selectedBotIndex = Integer.parseInt((String) table.getValueAt(selectionModel.getAnchorSelectionIndex(), 1));
      showBotUiOptions(selectedBotIndex);
    };
  }

  private int selectedPlayer = -1;

  private void showBotUiOptions(int playerIndex) {
    if (playerIndex != selectedPlayer)
      selectedPlayer = playerIndex;

    if (optionsPanel != null) {
      botPanel.remove(optionsPanel);
    }

    optionsPanel = createBotUiOptions(playerIndex);
    botPanel.add(optionsPanel);

    new Animator<>(optionsPanel, (s) -> s.setSize(s.getWidth() + 10, s.getHeight() + 10), s -> s.getWidth() > 200).animate();
  }

  private JPanel createBotUiOptions(int playerIndex) {
    int tableSelectedBotIndex = Integer.parseInt((String) botTable.getValueAt(botTable.getSelectedRow(), 1));
    Preconditions.checkState(tableSelectedBotIndex == playerIndex, "Table / Settings mismatch");

    JPanel optionsPanel = new JPanel();
    optionsPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
    optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
    optionsPanel.setBackground(Color.WHITE);

    JPanel botNamePanel = new JPanel();
    String botName = (String) botTable.getValueAt(botTable.getSelectedRow(), 0);
    JLabel botNameLabelPrefix = new JLabel("Settings for");
    JLabel botNameLabel = new JLabel(botName);
    botNameLabel.setForeground(APOLLO_RED);
    botNameLabel.setLayout(new BoxLayout(botNameLabel, BoxLayout.X_AXIS));
    botNamePanel.add(botNameLabelPrefix);
    botNamePanel.add(botNameLabel);
    botNamePanel.setOpaque(false);
    optionsPanel.add(botNamePanel);

    PerBotDebugOptions options = PerBotDebugOptions.get(playerIndex);

    JCheckBox freezeCarCheckbox = new JCheckBox("Freeze car", options.isFreezeCar());
    freezeCarCheckbox.addActionListener((e) ->
        options.setFreezeCar(freezeCarCheckbox.getModel().isSelected()));
    freezeCarCheckbox.setOpaque(false);
    optionsPanel.add(freezeCarCheckbox);

    JCheckBox renderAllDebugLinesCheckbox = new JCheckBox("Render all debug lines", options.isRenderAllDebugLines());
    renderAllDebugLinesCheckbox.addActionListener((e) ->
        options.setRenderAllDebugLinesEnabled(renderAllDebugLinesCheckbox.getModel().isSelected()));
    renderAllDebugLinesCheckbox.setOpaque(false);
    optionsPanel.add(renderAllDebugLinesCheckbox);

    JCheckBox renderDebugLines = new JCheckBox("Render plan lines", options.isRenderPlan());
    renderDebugLines.addActionListener((e) ->
        options.setRenderPlan(renderDebugLines.getModel().isSelected()));
    renderDebugLines.setOpaque(false);
    optionsPanel.add(renderDebugLines);


    JCheckBox trailRendererCheckbox = new JCheckBox("Trail Renderer", options.isTrailRendererEnabled());
    trailRendererCheckbox.addActionListener((e) ->
        options.setTrailRendererEnabled(trailRendererCheckbox.getModel().isSelected()));
    trailRendererCheckbox.setOpaque(false);
    trailRendererCheckbox.setEnabled(false);
    trailRendererCheckbox.setToolTipText("No longer works");
    optionsPanel.add(trailRendererCheckbox);

    JCheckBox renderBallCheckbox = new JCheckBox("Render Ball", options.isRenderBallPrediction());
    renderBallCheckbox.addActionListener((e) ->
        options.setRenderBallPrediction(renderBallCheckbox.getModel().isSelected()));
    renderBallCheckbox.setOpaque(false);
    optionsPanel.add(renderBallCheckbox);

    return optionsPanel;
  }

  private class Animator<T> implements ActionListener {

    private final T animationElement;
    private final Consumer<T> operator;
    private final Predicate<T> termationEvaluator;
    private final Timer timer;

    private Animator(T animationElement, Consumer<T> operator, Predicate<T> termationEvaluator) {
      this.animationElement = animationElement;
      this.operator = operator;
      this.termationEvaluator = termationEvaluator;
      timer = new Timer(60 / 1000, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      operator.accept(animationElement);
      if (termationEvaluator.test(animationElement)) {
        timer.stop();
        botPanel.repaint();
        frame.pack();
      }
    }

    public void animate() {
      timer.start();
    }
  }
}
