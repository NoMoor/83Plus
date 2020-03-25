package com.eru.rlbot.bot.main;

import static java.awt.Component.CENTER_ALIGNMENT;

import com.eru.rlbot.bot.flags.GlobalDebugOptions;
import com.eru.rlbot.bot.flags.PerBotDebugOptions;
import com.eru.rlbot.common.Pair;
import com.eru.rlbot.common.util.BuildInfo;
import com.google.common.base.Preconditions;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
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
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

/**
 * If you want to understand how the rocket is flying, you'll need buttons and a read-out.
 */
public class DSKY {

  private static final Color APOLLO_RED = new Color(200, 15, 15);

  private static final String ICON_PATH = "/logo.png";

  private final VehicleAssemblyBuilding vehicleAssemblyBuilding;
  private final Integer commChannel;
  private final JFrame dskyFrame;
  private JPanel rocketPanel;
  private JPanel optionsPanel;
  private JTable rocketTrackingTable;

  /** Assembles a display. */
  public static void assemble(VehicleAssemblyBuilding vehicleAssemblyBuilding, Integer port) {
    new DSKY(vehicleAssemblyBuilding, port).buildUi();
  }

  DSKY(VehicleAssemblyBuilding vehicleAssemblyBuilding, Integer commChannel) {
    this.vehicleAssemblyBuilding = vehicleAssemblyBuilding;
    this.commChannel = commChannel;
    this.dskyFrame = new JFrame("AGC");
    this.rocketPanel = new JPanel();
  }

  private void buildUi() {
    dskyFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    dskyFrame.setResizable(false);
    dskyFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource(ICON_PATH)));

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
    JLabel portLabel = new JLabel("Status: Go on channel " + commChannel, SwingConstants.CENTER);
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

    rocketPanel = new JPanel();
    initRocketPanel();
    settingsPanel.add(rocketPanel);

    panel.add(settingsPanel);
    dskyFrame.add(panel);

    registerBuildInfo(dskyFrame);

    dskyFrame.pack();
    dskyFrame.setVisible(true);
  }

  private void registerBuildInfo(JFrame frame) {
    JMenu versionMenu = new JMenu("Help");
    JMenuItem versionInfo = new JMenuItem("Version Info");
    versionInfo.setMnemonic('v');
    versionInfo.setAccelerator(KeyStroke.getKeyStroke("v"));
    versionInfo.addActionListener(e -> JOptionPane.showMessageDialog(
        frame, BuildInfo.getInfo().displayFormat(), "Build Info", JOptionPane.PLAIN_MESSAGE));
    versionMenu.add(versionInfo);

    if (frame.getJMenuBar() == null) {
      frame.setJMenuBar(new JMenuBar());
    }

    frame.getJMenuBar().add(Box.createHorizontalGlue());
    frame.getJMenuBar().add(versionMenu);
  }

  /**
   * Creates ui to adjust settings for the game / all rockets.
   */
  private JPanel initGlobalDebugPanel() {
    JPanel globalPanel = new JPanel();
    globalPanel.setOpaque(false);
    globalPanel.setLayout(new GridBagLayout());

    JPanel p1 = new JPanel();
    p1.setLayout(new BoxLayout(p1, BoxLayout.Y_AXIS));
    JCheckBox kickoffGameEnabled = new JCheckBox("Kickoff game", GlobalDebugOptions.isKickoffGameEnabled());
    kickoffGameEnabled.addActionListener((e) -> {
      boolean value = kickoffGameEnabled.getModel().isSelected();
      if (value) {
        int selection =
            JOptionPane.showConfirmDialog(
                dskyFrame,
                "Enabling kickoff game involves state setting. Do you want to allow state setting?",
                "Cheaters confirmation",
                JOptionPane.YES_NO_OPTION);
        boolean result = selection == JOptionPane.YES_OPTION;
        GlobalDebugOptions.setKickoffGameEnabled(result);
        kickoffGameEnabled.getModel().setSelected(result);
      } else {
        GlobalDebugOptions.setKickoffGameEnabled(false);
      }
    });
    kickoffGameEnabled.setOpaque(false);
    p1.add(kickoffGameEnabled);

    JCheckBox stateLoggerEnabled = new JCheckBox("State logger", GlobalDebugOptions.isStateLoggerEnabled());
    stateLoggerEnabled.addActionListener((e) ->
        GlobalDebugOptions.setStateLoggerEnabled(stateLoggerEnabled.getModel().isSelected()));
    stateLoggerEnabled.setOpaque(false);
    p1.setOpaque(false);
    p1.add(stateLoggerEnabled);

    JCheckBox renderStatsCheckbox = new JCheckBox("Render stats", GlobalDebugOptions.isRenderStats());
    renderStatsCheckbox.addActionListener((e) ->
        GlobalDebugOptions.setRenderStats(renderStatsCheckbox.getModel().isSelected()));
    renderStatsCheckbox.setOpaque(false);
    p1.add(renderStatsCheckbox);

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

  /**
   * Creates a panel showing all rockets.
   */
  private void initRocketPanel() {
    rocketPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    rocketPanel.setLayout(new BoxLayout(rocketPanel, BoxLayout.X_AXIS));
    rocketPanel.setOpaque(false);

    final List<Pair<Integer, Integer>> runningBots = new ArrayList<>();

    ActionListener myListener = e -> {
      // Only refresh the panel when the number of bots has changed.
      List<Pair<Integer, Integer>> currentRunningBots = vehicleAssemblyBuilding.getRocketList().stream()
          .filter(bot -> vehicleAssemblyBuilding.botManager.getRunningBotIndices().contains(bot.serialNumber))
          .map(bot -> Pair.of(bot.serialNumber, bot.team))
          .sorted(Comparator.comparing(Pair::getFirst))
          .collect(Collectors.toList());

      if (runningBots.equals(currentRunningBots)) {
        return;
      } else {
        runningBots.clear();
        runningBots.addAll(currentRunningBots);
      }

      // Clear the panel
      rocketPanel.removeAll();

      // Create a table
      rocketTrackingTable = new JTable();
      rocketTrackingTable.getTableHeader().setReorderingAllowed(false);
      DefaultTableModel tableModel = new DefaultTableModel();

      tableModel.addColumn("Call sign");
      tableModel.addColumn("Serial number");
      tableModel.addColumn("Alliance");

      JScrollPane tableScrollPane = new JScrollPane(rocketTrackingTable);
      tableScrollPane.setPreferredSize(new Dimension(200, 100));
      tableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      tableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

      rocketPanel.add(tableScrollPane);

      for (int index : vehicleAssemblyBuilding.botManager.getRunningBotIndices()) {
        ApolloGuidanceComputer bot = vehicleAssemblyBuilding.getBot(index);
        if (bot != null) {
          tableModel.addRow(new String[]{bot.getName(), String.valueOf(bot.getIndex()), bot.team == 0 ? "Blue" : "Orange"});
        }
      }

      rocketTrackingTable.setAutoCreateRowSorter(true);
      rocketTrackingTable.setModel(tableModel);
      rocketTrackingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

      rocketTrackingTable.getSelectionModel().addListSelectionListener(handleRocketSelection(rocketTrackingTable));
      rocketTrackingTable.setOpaque(false);
      tableScrollPane.setOpaque(false);

      rocketPanel.revalidate();
      rocketPanel.repaint();
      dskyFrame.pack();
    };

    new Timer(1000, myListener).start();
  }

  /**
   * Updates the displays to show details of the given rocket.
   */
  private ListSelectionListener handleRocketSelection(JTable rocketTable) {
    return (selectionEvent) -> {
      if (selectionEvent.getValueIsAdjusting())
        return;

      DefaultListSelectionModel selectionModel = (DefaultListSelectionModel) selectionEvent.getSource();
      int serialNumber = Integer.parseInt(
          (String) rocketTable.getValueAt(selectionModel.getAnchorSelectionIndex(), 1));
      showRocketDetails(serialNumber);
    };
  }

  private int selectedPlayer = -1;

  private void showRocketDetails(int serialNumber) {
    if (serialNumber != selectedPlayer)
      selectedPlayer = serialNumber;

    if (optionsPanel != null) {
      rocketPanel.remove(optionsPanel);
    }

    optionsPanel = createBotUiOptions(serialNumber);
    rocketPanel.add(optionsPanel);

    new Animator<>(optionsPanel, (s) -> s.setSize(s.getWidth() + 10, s.getHeight() + 10), s -> s.getWidth() > 200).animate();
  }

  private JPanel createBotUiOptions(int serialNumber) {
    int tableSelectedBotIndex = Integer.parseInt((String) rocketTrackingTable.getValueAt(rocketTrackingTable.getSelectedRow(), 1));
    Preconditions.checkState(tableSelectedBotIndex == serialNumber, "Table / Settings mismatch");

    JPanel optionsPanel = new JPanel();
    optionsPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
    optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
    optionsPanel.setBackground(Color.WHITE);

    JPanel botNamePanel = new JPanel();
    String botName = (String) rocketTrackingTable.getValueAt(rocketTrackingTable.getSelectedRow(), 0);
    JLabel botNameLabelPrefix = new JLabel("Configuration for");
    JLabel botNameLabel = new JLabel(botName);
    botNameLabel.setForeground(APOLLO_RED);
    botNameLabel.setLayout(new BoxLayout(botNameLabel, BoxLayout.X_AXIS));
    botNamePanel.add(botNameLabelPrefix);
    botNamePanel.add(botNameLabel);
    botNamePanel.setOpaque(false);
    optionsPanel.add(botNamePanel);

    PerBotDebugOptions options = PerBotDebugOptions.get(serialNumber);

    JCheckBox freezeCarCheckbox = new JCheckBox("Immobilize car", options.isImmobilizeCar());
    freezeCarCheckbox.addActionListener((e) ->
        options.setImmobilizeCar(freezeCarCheckbox.getModel().isSelected()));
    freezeCarCheckbox.setOpaque(false);
    optionsPanel.add(freezeCarCheckbox);

    JCheckBox renderAllDebugLinesCheckbox = new JCheckBox("Render all debug lines", options.isRenderAllDebugLines());
    renderAllDebugLinesCheckbox.addActionListener((e) ->
        options.setRenderAllDebugLinesEnabled(renderAllDebugLinesCheckbox.getModel().isSelected()));
    renderAllDebugLinesCheckbox.setOpaque(false);
    optionsPanel.add(renderAllDebugLinesCheckbox);

    JCheckBox renderDebugLines = new JCheckBox("Render paths", options.isRenderLines());
    renderDebugLines.addActionListener((e) ->
        options.setRenderLines(renderDebugLines.getModel().isSelected()));
    renderDebugLines.setOpaque(false);
    optionsPanel.add(renderDebugLines);

    JCheckBox renderRotations = new JCheckBox("Render rotations", options.isRenderRotationsEnabled());
    renderRotations.addActionListener((e) ->
        options.setRenderRotationsEnabled(renderRotations.getModel().isSelected()));
    renderRotations.setOpaque(false);
    optionsPanel.add(renderRotations);

    JCheckBox renderText = new JCheckBox("Render text", options.isRenderDebugText());
    renderText.addActionListener((e) ->
        options.setRenderDebugText(renderText.getModel().isSelected()));
    renderText.setOpaque(false);
    optionsPanel.add(renderText);

    JCheckBox renderBallCheckbox = new JCheckBox("Render ball prediction", options.isRenderBallPrediction());
    renderBallCheckbox.addActionListener((e) ->
        options.setRenderBallPrediction(renderBallCheckbox.getModel().isSelected()));
    renderBallCheckbox.setOpaque(false);
    optionsPanel.add(renderBallCheckbox);

    JCheckBox renderCarPredictionsCheckbox =
        new JCheckBox("Render car predictions", options.isRenderCarPredictionsEnabled());
    renderCarPredictionsCheckbox.addActionListener((e) ->
        options.setRenderCarPredictions(renderCarPredictionsCheckbox.getModel().isSelected()));
    renderCarPredictionsCheckbox.setOpaque(false);
    optionsPanel.add(renderCarPredictionsCheckbox);

    JCheckBox trailRendererCheckbox = new JCheckBox("Trail Renderer", options.isRenderCarTrails());
    trailRendererCheckbox.addActionListener((e) ->
        options.setRenderCarTrails(trailRendererCheckbox.getModel().isSelected()));
    trailRendererCheckbox.setOpaque(false);
    trailRendererCheckbox.setEnabled(false);
    trailRendererCheckbox.setToolTipText("No longer works");
    optionsPanel.add(trailRendererCheckbox);

    return optionsPanel;
  }

  // TODO: I don't think this works.
  private class Animator<T> implements ActionListener {
    private final T animationElement;
    private final Consumer<T> operator;
    private final Predicate<T> terminationEvaluator;
    private final Timer timer;

    private Animator(T animationElement, Consumer<T> operator, Predicate<T> terminationEvaluator) {
      this.animationElement = animationElement;
      this.operator = operator;
      this.terminationEvaluator = terminationEvaluator;
      timer = new Timer(60 / 1000, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      operator.accept(animationElement);
      if (terminationEvaluator.test(animationElement)) {
        timer.stop();
        rocketPanel.repaint();
        dskyFrame.pack();
      }
    }

    public void animate() {
      timer.start();
    }
  }
}
