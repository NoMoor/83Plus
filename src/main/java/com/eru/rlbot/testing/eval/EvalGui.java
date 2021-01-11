package com.eru.rlbot.testing.eval;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.common.ScenarioProtos;
import com.eru.rlbot.common.ScenarioProtos.Scenario;
import com.eru.rlbot.common.ScenarioProtos.Suite;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.flatbuffers.FlatBufferBuilder;
import com.google.protobuf.util.JsonFormat;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rlbot.cppinterop.RLBotDll;
import rlbot.cppinterop.RLBotInterfaceException;
import rlbot.gamestate.GameState;
import rlbot.render.RenderPacket;
import rlbot.render.Renderer;

/**
 * A GUI to create and execute unit test suites for a bot.
 *
 * <p>A suite consists of a list of scenarios, each of which can be turned on or off. Each scenario has a starting
 * state for cars and ball, a name, timeout, and success criteria.
 *
 * <p>Executing a suite will start a loop through each scenario which is turned on. Each Scenario will be run in series.
 * Once each scenario is run, the suite will be restarted.
 */
public class EvalGui {

  private static final Logger log2Console = LogManager.getLogger("EvalGuiConsole");

  private static final EvalRenderer evalRenderer = new EvalRenderer();

  private static final LinkedHashMap<Long, Scenario> scenarioLibrary = new LinkedHashMap<>();
  private static final LinkedHashMap<Long, Suite> suiteLibrary = new LinkedHashMap<>();

  private static SpinnerNumberModel bvxModel;
  private static SpinnerNumberModel bxModel;
  private static SpinnerNumberModel bvyModel;
  private static SpinnerNumberModel byModel;
  private static SpinnerNumberModel bvzModel;
  private static SpinnerNumberModel bzModel;

  private static SpinnerNumberModel cPitchModel;
  private static SpinnerNumberModel cYawModel;
  private static SpinnerNumberModel cRollModel;
  private static SpinnerNumberModel cvxModel;
  private static SpinnerNumberModel cxModel;
  private static SpinnerNumberModel cvyModel;
  private static SpinnerNumberModel cyModel;
  private static SpinnerNumberModel cvzModel;
  private static SpinnerNumberModel czModel;
  private static SpinnerNumberModel boostModel;

  private static JTable suiteLibraryTable;
  private static DefaultTableModel suiteLibraryTableModel;
  private static JTable suiteScenarioTable;
  private static DefaultTableModel suiteScenarioTableModel;
  private static JTable scenarioLibraryTable;
  private static DefaultTableModel scenarioLibraryTableModel;

  private static JTextField nameField;
  private static JButton startButton;
  private static JButton stopButton;

  private static JButton removeSuiteScenarioButton;
  private static JButton addSuiteScenarioButton;
  private static SpinnerNumberModel timeoutModel;
  private static Timer evalTimer;

  // Table
  // Input elements
  // Read from file
  // Write to file
  public static void show() {
    JFrame frame = new JFrame("Eval");
    frame.setSize(1000, 700);
    frame.setVisible(true);

    // Suite Library
    suiteLibraryTable = new JTable();
    suiteLibraryTableModel = new DefaultTableModel() {
      @Override
      public int getRowCount() {
        return suiteLibrary.size();
      }

      @Override
      public Object getValueAt(int row, int column) {
        Suite suite = Iterables.get(suiteLibrary.values(), row);

        switch (column) {
          case 0:
            return String.valueOf(suite.getId());
          case 1:
            return suite.getName();
          case 2:
          default:
            long enabledEntries = suite.getEntriesList().stream().filter(Suite.Entry::getEnabled).count();
            long entryCount = suite.getEntriesCount();
            return (enabledEntries != entryCount) ? String.format("%d/%d", enabledEntries, entryCount) : entryCount;
        }
      }
    };

    suiteLibraryTableModel.addColumn("Suite #");
    suiteLibraryTableModel.addColumn("Suite Name");
    suiteLibraryTableModel.addColumn("Scenario count");
    suiteLibraryTable.setModel(suiteLibraryTableModel);
    suiteLibraryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    JScrollPane suiteTableScrollPane = new JScrollPane(suiteLibraryTable);
    suiteTableScrollPane.setPreferredSize(new Dimension(400, 200));

    JPanel suiteLibraryButtonPanel = new JPanel();
    suiteLibraryButtonPanel.setLayout(new BoxLayout(suiteLibraryButtonPanel, BoxLayout.Y_AXIS));
    JButton addSuiteButton = new JButton("+");
    addSuiteButton.setPreferredSize(new Dimension(50, 40));
    addSuiteButton.addActionListener(e -> {
      String name = JOptionPane.showInputDialog("Suite Name");
      if (name != null) {
        long nextId = nextSuiteId();
        if (name.isEmpty()) {
          name = "Suite " + nextId;
        }
        Suite newSuite = Suite.newBuilder()
            .setId(nextId)
            .setName(name)
            .build();
        addSuite(newSuite);
      }
    });
    JButton removeSuiteButton = new JButton("-");
    removeSuiteButton.setEnabled(false);
    removeSuiteButton.setPreferredSize(new Dimension(50, 40));
    removeSuiteButton.addActionListener(e -> {
      int selectionIndex = suiteLibraryTable.getSelectionModel().getAnchorSelectionIndex();
      if (selectionIndex == -1) {
        return;
      }
      removeSuite(selectionIndex);
    });
    suiteLibraryTable.getSelectionModel().addListSelectionListener(e -> {
      if (e.getValueIsAdjusting()) {
        return;
      }

      int selectionIndex = suiteLibraryTable.getSelectionModel().getAnchorSelectionIndex();
      removeSuiteButton.setEnabled(selectionIndex != -1);
      startButton.setEnabled(selectionIndex != -1);
      updateSuiteScenarioButtonStates();
      suiteScenarioTableModel.fireTableDataChanged();
    });
    suiteLibraryButtonPanel.add(addSuiteButton);
    suiteLibraryButtonPanel.add(removeSuiteButton);


    // Suite Scenario List
    suiteScenarioTable = new JTable();
    suiteScenarioTableModel = new DefaultTableModel() {
      @Override
      public int getRowCount() {
        Suite selectedSuite = getSelectedSuite();
        return selectedSuite == null ? 0 : selectedSuite.getEntriesCount();
      }

      @Override
      public Object getValueAt(int row, int column) {
        Suite selectedSuite = getSelectedSuite();
        Suite.Entry suiteEntry = selectedSuite.getEntries(row);

        switch (column) {
          case 0:
            return suiteEntry.getEnabled();
          case 1:
            return String.valueOf(suiteEntry.getScenarioId());
          default:
          case 2:
            return scenarioLibrary.get(suiteEntry.getScenarioId()).getName();
        }
      }

      @Override
      public void setValueAt(Object value, int row, int column) {
        Suite selectedSuite = getSelectedSuite();
        Suite.Entry suiteEntry = selectedSuite.getEntries(row);
        switch (column) {
          case 0:
            updateSuiteEntry(suiteEntry.toBuilder()
                .setEnabled((boolean) value)
                .build());
          case 1:
            return;
          default:
          case 2:
            Scenario scenario = getScenario(suiteEntry);

            if (scenario == null) {
              return;
            }

            updateScenario(scenario.toBuilder().setName((String) value).build());
        }
      }

      @Override
      public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
          case 0:
            return Boolean.class;
          case 1:
          case 2:
          default:
            return String.class;
        }
      }
    };

    suiteScenarioTableModel.addColumn("Enabled");
    suiteScenarioTableModel.addColumn("Scenario #");
    suiteScenarioTableModel.addColumn("Scenario Name");
    suiteScenarioTable.setModel(suiteScenarioTableModel);
    suiteScenarioTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    suiteScenarioTable.getSelectionModel().addListSelectionListener(e -> {
      if (e.getValueIsAdjusting()) {
        return;
      }

      updateSuiteScenarioButtonStates();
      int index = getSelectedSuiteScenarioIndex();
      if (index == -1) {
        return;
      }

      Suite selectedSuite = getSelectedSuite();
      if (selectedSuite != null && selectedSuite.getEntriesCount() > index) {
        Suite.Entry entry = selectedSuite.getEntries(index);

        int associatedScenarioIndex =
            Iterables.indexOf(scenarioLibrary.values(), scenario -> scenario.getId() == entry.getScenarioId());
        scenarioLibraryTable.getSelectionModel().setSelectionInterval(associatedScenarioIndex, associatedScenarioIndex);
      }
    });

    JScrollPane suiteScenarioTableScrollPane = new JScrollPane(suiteScenarioTable);
    suiteScenarioTableScrollPane.setPreferredSize(new Dimension(400, 200));

    JPanel suiteScenarioButtonPanel = new JPanel();
    suiteScenarioButtonPanel.setLayout(new BoxLayout(suiteScenarioButtonPanel, BoxLayout.Y_AXIS));
    addSuiteScenarioButton = new JButton("+");
    addSuiteScenarioButton.setEnabled(false);
    addSuiteScenarioButton.addActionListener(e -> {
      int index = getSelectedSuiteScenarioIndex();
      Suite suite = getSelectedSuite();
      Scenario scenario = getSelectedScenario();
      if (scenario == null || suite == null) {
        return;
      }

      Suite updatedSuite = suite.toBuilder()
          .addEntries(Suite.Entry.newBuilder()
              .setId(nextSuiteEntyId())
              .setEnabled(true)
              .setScenarioId(scenario.getId()))
          .build();

      updateSuite(updatedSuite);
      suiteScenarioTable.getSelectionModel().setSelectionInterval(index, index);
    });
    removeSuiteScenarioButton = new JButton("-");
    removeSuiteScenarioButton.setEnabled(false);
    removeSuiteScenarioButton.addActionListener(e -> {
      int selectedRow = suiteScenarioTable.getSelectedRow();
      Suite suite = getSelectedSuite();

      if (selectedRow == -1 || suite == null) {
        return;
      }

      Suite updatedSuite = suite.toBuilder()
          .removeEntries(selectedRow)
          .build();

      updateSuite(updatedSuite);
    });
    suiteScenarioButtonPanel.add(addSuiteScenarioButton);
    suiteScenarioButtonPanel.add(removeSuiteScenarioButton);

    // Scenario Library
    scenarioLibraryTable = new JTable();
    scenarioLibraryTableModel = new DefaultTableModel() {
      @Override
      public int getRowCount() {
        return scenarioLibrary.size();
      }

      @Override
      public String getValueAt(int row, int column) {
        Scenario scenario = Iterables.get(scenarioLibrary.values(), row);

        switch (column) {
          case 0:
            return String.valueOf(scenario.getId());
          case 1:
            return scenario.getName();
          case 2:
          default:
            return Utils.toString(scenario.getBall());
        }
      }
    };

    scenarioLibraryTableModel.addColumn("Scenario #");
    scenarioLibraryTableModel.addColumn("Scenario Name");
    scenarioLibraryTableModel.addColumn("Ball Info");
    scenarioLibraryTable.setModel(scenarioLibraryTableModel);
    scenarioLibraryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    scenarioLibraryTable.getSelectionModel().addListSelectionListener(EvalGui::updatePickers);

    JScrollPane scenarioLibraryTableScrollPane = new JScrollPane(scenarioLibraryTable);
    scenarioLibraryTableScrollPane.setPreferredSize(new Dimension(400, 200));

    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    JPanel suitePanel = new JPanel();

    // Start/Stop button panel
    JPanel startStopButtonPanel = new JPanel();
    startStopButtonPanel.setLayout(new BoxLayout(startStopButtonPanel, BoxLayout.Y_AXIS));
    startButton = new JButton("Start");
    startButton.addActionListener(EvalGui::startEval);
    startButton.setEnabled(false);

    stopButton = new JButton("Stop");
    stopButton.addActionListener(EvalGui::stopEval);
    stopButton.setEnabled(false);

    startStopButtonPanel.add(startButton);
    startStopButtonPanel.add(stopButton);

    suitePanel.add(startStopButtonPanel);
    suitePanel.add(suiteTableScrollPane);
    suitePanel.add(suiteLibraryButtonPanel);
    suitePanel.add(suiteScenarioTableScrollPane);
    suitePanel.add(suiteScenarioButtonPanel);

    JPanel scenarioPanel = new JPanel();
    scenarioPanel.add(scenarioLibraryTableScrollPane);
    scenarioPanel.add(createSetterPanel());

    panel.add(suitePanel);
    panel.add(scenarioPanel);

    frame.add(panel);
  }

  private static void updateScenario(Scenario scenario) {
    int index = Iterables.indexOf(scenarioLibrary.values(), entry -> entry.getId() == scenario.getId());
    if (index == -1) {
      return;
    }

    // Retain scenario library selection
    // Retain Suite scenario selection
    int suiteScenarioIndex = getSelectedSuiteScenarioIndex();

    Scenario selectedScenario = getSelectedScenario();

    scenarioLibrary.put(scenario.getId(), scenario);

    if (suiteScenarioIndex != -1) {
      suiteScenarioTable.getSelectionModel().setSelectionInterval(suiteScenarioIndex, suiteScenarioIndex);
    }

    if (selectedScenario != null) {
      scenarioLibraryTable.getSelectionModel().setSelectionInterval(index, index);
    }
  }

  private static void updateSuiteEntry(Suite.Entry updatedEntry) {
    int suiteIndex = getSelectedSuiteIndex();
    Suite suite = getSelectedSuite();

    int suiteScenarioIndex = Iterables.indexOf(suite.getEntriesList(), entry -> entry.getId() == updatedEntry.getId());

    Suite updatedSuite = suite.toBuilder()
        .removeEntries(suiteScenarioIndex)
        .addEntries(suiteScenarioIndex, updatedEntry)
        .build();
    updateSuite(updatedSuite);

    if (suiteIndex != -1) {
      suiteLibraryTable.getSelectionModel().setSelectionInterval(suiteIndex, suiteIndex);
    }
    if (suiteScenarioIndex != -1) {
      suiteScenarioTable.getSelectionModel().setSelectionInterval(suiteScenarioIndex, suiteScenarioIndex);
    }
  }

  private static int getSelectedSuiteIndex() {
    return suiteLibraryTable.getSelectedRow();
  }

  private static Suite getSelectedSuite() {
    int selectedRow = getSelectedSuiteIndex();
    return selectedRow == -1 ? null : Iterables.get(suiteLibrary.values(), selectedRow);
  }

  private static void updateSuiteScenarioButtonStates() {
    Suite suite = getSelectedSuite();
    Scenario scenario = getSelectedScenario();

    addSuiteScenarioButton.setEnabled(suite != null && scenario != null);
    removeSuiteScenarioButton.setEnabled(getSelectedSuiteScenario() != null);
  }

  private static Scenario getSelectedScenario() {
    int selection = scenarioLibraryTable.getSelectionModel().getAnchorSelectionIndex();
    return selection == -1 ? null : Iterables.get(scenarioLibrary.values(), selection);
  }

  private static int getSelectedSuiteScenarioIndex() {
    return suiteScenarioTable.getSelectedRow();
  }

  private static Suite.Entry getSelectedSuiteScenario() {
    int selectedRow = getSelectedSuiteScenarioIndex();
    if (selectedRow == -1) {
      return null;
    }
    Suite selectedSuite = getSelectedSuite();

    return selectedSuite == null || selectedSuite.getEntriesCount() <= selectedRow
        ? null
        : selectedSuite.getEntries(selectedRow);
  }

  private static void addSuite(Suite suite) {
    suiteLibrary.put(suite.getId(), suite);
    suiteLibraryTableModel.fireTableDataChanged();
    writeToFile();
  }

  private static void removeSuite(int index) {
    suiteLibrary.remove(Iterables.get(suiteLibrary.values(), index).getId());
    suiteLibraryTableModel.fireTableDataChanged();
    writeToFile();
  }

  private static void updateSuite(Suite updatedSuite) {
    int updatedIndex = Iterables.indexOf(suiteLibrary.values(), suite -> suite.getId() == updatedSuite.getId());

    suiteLibrary.put(updatedSuite.getId(), updatedSuite);
    suiteLibraryTableModel.fireTableDataChanged();

    suiteLibraryTable.getSelectionModel().setSelectionInterval(updatedIndex, updatedIndex);

    writeToFile();
  }

  private static void startEval(ActionEvent actionEvent) {
    Suite selectedSuite = getSelectedSuite();
    if (selectedSuite == null) {
      return;
    }

    stopButton.setEnabled(true);

    evalTimer = new Timer();
    evalTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        doEval();
      }
    }, 0, 5);
  }

  private static volatile ImmutableList<Suite.Entry> evalList;
  private static volatile List<Boolean> evalResultList;
  private static volatile int currentIndex = 0;
  private static volatile float startTime = 0;
  private static volatile int longestScenarioName = 0;
  private static boolean needsStart;

  private static void doEval() {
    // TODO: Timer that gets called back every 5ms.

    Suite selectedSuite = getSelectedSuite();

    // Check that the list of ones to run is the same.
    ImmutableList<Suite.Entry> newEvalList = selectedSuite.getEntriesList()
        .stream()
        .filter(Suite.Entry::getEnabled)
        .collect(toImmutableList());

    if (evalList == null || !evalList.equals(newEvalList)) {
      evalList = newEvalList;
      evalResultList = new ArrayList<>(evalList.size());
      currentIndex = 0;
      needsStart = true;

      longestScenarioName = evalList.stream()
          .map(Suite.Entry::getScenarioId)
          .map(scenarioLibrary::get)
          .map(Scenario::getName)
          .mapToInt(String::length)
          .max()
          .orElse(0);
    }

    Suite.Entry currentEntry = evalList.get(currentIndex);
    Scenario scenario = getScenario(currentEntry);

    boolean isScenarioDone = false;
    if (scenario == null) {
      isScenarioDone = true;
    } else {
      if (needsStart) {
        // Start next scenario
        RLBotDll.setGameState(new GameState()
            .withBallState(Utils.toDesired(scenario.getBall()))
            .withCarState(0, Utils.toDesired(scenario.getCar(0)))
            .buildPacket());
        startTime = getCurrentGameTime();
        needsStart = false;
      }
      // TODO: Evaluate the scenario

      // Check Timer
      if (startTime + (scenario.getTimeOutMs() / 1000.0f) <= getCurrentGameTime()) {
        evalResultList.add(Boolean.FALSE);
        isScenarioDone = true;
      }
    }

    if (isScenarioDone) {
      currentIndex = (currentIndex + 1) % evalList.size();
      needsStart = true;

      if (currentIndex == 0) {
        evalResultList.clear();
      }
    }

    renderEval();
  }

  private static void renderEval() {
    evalRenderer.initTick();

    for (int i = 0; i < evalList.size(); i++) {
      Suite.Entry entry = evalList.get(i);

      Scenario scenario = getScenario(entry);
      if (scenario == null) {
        continue;
      }

      int y = i * 25;
      evalRenderer.render2dText(Color.gray, 0, y, scenario.getName());

      int startX = longestScenarioName * 30;
      if (evalResultList.size() <= i) {
        evalRenderer.render2dText(Color.yellow, startX, y, "-");
      } else {
        evalRenderer.render2dText(evalResultList.get(i) ? Color.green : Color.red, startX, y, evalResultList.get(i) ? "Pass" : "Fail");
      }
    }

    evalRenderer.sendData();
  }

  private static Scenario getScenario(Suite.Entry entry) {
    return scenarioLibrary.get(entry.getScenarioId());
  }

  private static float lastTimeSeen = 0;

  private static float getCurrentGameTime() {
    try {
      lastTimeSeen = RLBotDll.getCurrentFlatbufferPacket().gameInfo().secondsElapsed();
    } catch (RLBotInterfaceException e) {
      e.printStackTrace();
    }
    return lastTimeSeen;
  }

  private static void stopEval(ActionEvent actionEvent) {
    if (evalTimer != null) {
      evalTimer.cancel();
      evalTimer = null;
    }
    stopButton.setEnabled(false);
  }

  private static void updatePickers(ListSelectionEvent listSelectionEvent) {
    if (listSelectionEvent.getValueIsAdjusting()) {
      return;
    }

    updateSuiteScenarioButtonStates();

    Scenario scenario = getSelectedScenario();
    if (scenario == null) {
      return;
    }

    nameField.setText(scenario.getName());
    bxModel.setValue(scenario.getBall().getPos(0));
    byModel.setValue(scenario.getBall().getPos(1));
    bzModel.setValue(scenario.getBall().getPos(2));
    bvxModel.setValue(scenario.getBall().getVel(0));
    bvyModel.setValue(scenario.getBall().getVel(1));
    bvzModel.setValue(scenario.getBall().getVel(2));

    cxModel.setValue(scenario.getCar(0).getPos(0));
    cyModel.setValue(scenario.getCar(0).getPos(1));
    czModel.setValue(scenario.getCar(0).getPos(2));
    cvxModel.setValue(scenario.getCar(0).getVel(0));
    cvyModel.setValue(scenario.getCar(0).getVel(1));
    cvzModel.setValue(scenario.getCar(0).getVel(2));

    cPitchModel.setValue(scenario.getCar(0).getOrientation(0));
    cYawModel.setValue(scenario.getCar(0).getOrientation(1));
    cRollModel.setValue(scenario.getCar(0).getOrientation(2));

    boostModel.setValue(scenario.getCar(0).getBoost());
    timeoutModel.setValue(scenario.getTimeOutMs() / 1000);
  }

  private static JPanel createSetterPanel() {
    JPanel creationPanel = new JPanel();

    creationPanel.setLayout(new BoxLayout(creationPanel, BoxLayout.Y_AXIS));

    JPanel namePanel = new JPanel();
    JLabel nameLabel = new JLabel("Scenario Name");
    nameField = new JTextField(30);
    namePanel.add(nameLabel);
    namePanel.add(nameField);

    creationPanel.add(namePanel);

    JPanel ballPos = new JPanel();

    // x, y, z for ball
    JLabel xlabel = new JLabel("Ball X");
    bxModel = new SpinnerNumberModel(0, -Constants.HALF_WIDTH + Constants.BALL_RADIUS, Constants.HALF_WIDTH - Constants.BALL_RADIUS, 20);
    JSpinner xspinner = new JSpinner(bxModel);

    JLabel ylabel = new JLabel("Ball Y");
    byModel = new SpinnerNumberModel(0, -Constants.HALF_LENGTH + Constants.BALL_RADIUS, Constants.HALF_LENGTH - Constants.BALL_RADIUS, 20);
    JSpinner yspinner = new JSpinner(byModel);

    JLabel zlabel = new JLabel("Ball Z");
    bzModel = new SpinnerNumberModel(Constants.BALL_RADIUS, 0 + Constants.BALL_RADIUS, Constants.FIELD_HEIGHT - Constants.BALL_RADIUS, 20);
    JSpinner zspinner = new JSpinner(bzModel);

    ballPos.add(xlabel);
    ballPos.add(xspinner);
    ballPos.add(ylabel);
    ballPos.add(yspinner);
    ballPos.add(zlabel);
    ballPos.add(zspinner);

    JPanel ballVel = new JPanel();

    // x, y, z for ball
    JLabel vxlabel = new JLabel("Ball vX");
    bvxModel = new SpinnerNumberModel(0, -Constants.BOOSTED_MAX_SPEED, Constants.BOOSTED_MAX_SPEED, 20);
    JSpinner vxspinner = new JSpinner(bvxModel);

    JLabel vylabel = new JLabel("Ball vY");
    bvyModel = new SpinnerNumberModel(0, -Constants.BOOSTED_MAX_SPEED, Constants.BOOSTED_MAX_SPEED, 20);
    JSpinner vyspinner = new JSpinner(bvyModel);

    JLabel vzlabel = new JLabel("Ball vZ");
    bvzModel = new SpinnerNumberModel(0, -Constants.BOOSTED_MAX_SPEED, Constants.BOOSTED_MAX_SPEED, 20);
    JSpinner vzspinner = new JSpinner(bvzModel);

    ballVel.add(vxlabel);
    ballVel.add(vxspinner);
    ballVel.add(vylabel);
    ballVel.add(vyspinner);
    ballVel.add(vzlabel);
    ballVel.add(vzspinner);

    creationPanel.add(ballPos);
    creationPanel.add(ballVel);


    // Car pickers
    // Car Pos

    JPanel carPos = new JPanel();
    JLabel cxlabel = new JLabel("Car X");
    cxModel = new SpinnerNumberModel(0, -Constants.HALF_WIDTH, Constants.HALF_WIDTH, 20);
    JSpinner cxspinner = new JSpinner(cxModel);

    JLabel cylabel = new JLabel("Car Y");
    cyModel = new SpinnerNumberModel(0, -Constants.HALF_LENGTH, Constants.HALF_LENGTH, 20);
    JSpinner cyspinner = new JSpinner(cyModel);

    JLabel czlabel = new JLabel("Car Z");
    czModel = new SpinnerNumberModel(Constants.CAR_AT_REST, Constants.CAR_AT_REST, Constants.FIELD_HEIGHT, 20);
    JSpinner czspinner = new JSpinner(czModel);
    carPos.add(cxlabel);
    carPos.add(cxspinner);
    carPos.add(cylabel);
    carPos.add(cyspinner);
    carPos.add(czlabel);
    carPos.add(czspinner);

    // Car Vel
    JPanel carVel = new JPanel();
    JLabel cvxlabel = new JLabel("Car vX");
    cvxModel = new SpinnerNumberModel(0, -Constants.BOOSTED_MAX_SPEED, Constants.BOOSTED_MAX_SPEED, 20);
    JSpinner cvxspinner = new JSpinner(cvxModel);

    JLabel cvylabel = new JLabel("Car vY");
    cvyModel = new SpinnerNumberModel(0, -Constants.BOOSTED_MAX_SPEED, Constants.BOOSTED_MAX_SPEED, 20);
    JSpinner cvyspinner = new JSpinner(cvyModel);

    JLabel cvzlabel = new JLabel("Car vZ");
    cvzModel = new SpinnerNumberModel(0, -Constants.BOOSTED_MAX_SPEED, Constants.BOOSTED_MAX_SPEED, 20);
    JSpinner cvzspinner = new JSpinner(cvzModel);
    carVel.add(cvxlabel);
    carVel.add(cvxspinner);
    carVel.add(cvylabel);
    carVel.add(cvyspinner);
    carVel.add(cvzlabel);
    carVel.add(cvzspinner);

    // Car Orientation
    JPanel carOr = new JPanel();
    JLabel cpitchlabel = new JLabel("Car pitch");
    cPitchModel = new SpinnerNumberModel(0, -Math.PI, Math.PI, Math.PI / 4);
    JSpinner cpitchspinner = new JSpinner(cPitchModel);

    JLabel cyawlabel = new JLabel("Car yaw");
    cYawModel = new SpinnerNumberModel(0, -Math.PI, Math.PI, Math.PI / 4);
    JSpinner cyawspinner = new JSpinner(cYawModel);

    JLabel crolllabel = new JLabel("Car roll");
    cRollModel = new SpinnerNumberModel(0, -Math.PI, Math.PI, Math.PI / 4);
    JSpinner crollspinner = new JSpinner(cRollModel);
    carOr.add(cpitchlabel);
    carOr.add(cpitchspinner);
    carOr.add(cyawlabel);
    carOr.add(cyawspinner);
    carOr.add(crolllabel);
    carOr.add(crollspinner);

    // Car Boost
    JPanel carBoost = new JPanel();
    JLabel cBoostLabel = new JLabel("Car boost");
    boostModel = new SpinnerNumberModel(100, 0, 100, 10);
    JSpinner cBoostSpinner = new JSpinner(boostModel);
    carBoost.add(cBoostLabel);
    carBoost.add(cBoostSpinner);

    creationPanel.add(carPos);
    creationPanel.add(carVel);
    creationPanel.add(carOr);
    creationPanel.add(carBoost);

    // Scenario Timeout
    JPanel timeoutPanel = new JPanel();
    JLabel timeoutLabel = new JLabel("Timeout (s)");
    timeoutModel = new SpinnerNumberModel(5, -1, 60, 1);
    JSpinner timeoutSpinner = new JSpinner(timeoutModel);
    timeoutPanel.add(timeoutLabel);
    timeoutPanel.add(timeoutSpinner);

    JButton createButton = new JButton("Create");
    createButton.addActionListener(e -> {
      long nextId = nextScenarioId();
      scenarioLibrary.put(nextId,
          fromFields()
              .setId(nextId)
              .setCreated(System.currentTimeMillis())
              .build());
      scenarioLibraryTableModel.fireTableDataChanged();
      writeToFile();
    });

    JButton updateButton = new JButton("Update");
    updateButton.addActionListener(e -> {
      int selectedRow = scenarioLibraryTable.getSelectedRow();

      if (selectedRow != -1) {
        Scenario oldScenario = Iterables.get(scenarioLibrary.values(), selectedRow);
        Scenario scenario = fromFields().setId(oldScenario.getId())
            .setUpdated(System.currentTimeMillis())
            .build();

        scenarioLibrary.put(scenario.getId(), scenario);
        scenarioLibraryTableModel.fireTableDataChanged();
        suiteScenarioTableModel.fireTableDataChanged();
        scenarioLibraryTable.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
        writeToFile();
      }
    });

    creationPanel.add(timeoutPanel);
    creationPanel.add(createButton);
    creationPanel.add(updateButton);

    return creationPanel;
  }

  private static Scenario.Builder fromFields() {
    Scenario.BallState data = Scenario.BallState.newBuilder()
        .addPos(bxModel.getNumber().floatValue())
        .addPos(byModel.getNumber().floatValue())
        .addPos(bzModel.getNumber().floatValue())
        .addVel(bvxModel.getNumber().floatValue())
        .addVel(bvyModel.getNumber().floatValue())
        .addVel(bvzModel.getNumber().floatValue())
        .build();

    Scenario.CarState car = Scenario.CarState.newBuilder()
        .setId(0)
        .setTeam(0)
        .addPos(cxModel.getNumber().floatValue())
        .addPos(cyModel.getNumber().floatValue())
        .addPos(czModel.getNumber().floatValue())
        .addVel(cvxModel.getNumber().floatValue())
        .addVel(cvyModel.getNumber().floatValue())
        .addVel(cvzModel.getNumber().floatValue())
        .addOrientation(cPitchModel.getNumber().floatValue())
        .addOrientation(cYawModel.getNumber().floatValue())
        .addOrientation(cRollModel.getNumber().floatValue())
        .setBoost(boostModel.getNumber().intValue())
        .addSpin(0)
        .addSpin(0)
        .addSpin(0)
        .build();

    return Scenario.newBuilder()
        .setId(nextScenarioId())
        .setName(nameField.getText().equals("") ? "Scenario " + scenarioLibrary.size() : nameField.getText())
        .setTimeOutMs((int) timeoutModel.getNumber().floatValue() * 1000)
        .setBall(data)
        .addCar(car);
  }

  private static long nextSuiteId() {
    return suiteLibrary.values().stream()
        .mapToLong(Suite::getId)
        .max()
        .orElse(-1L) + 1;
  }

  private static long nextSuiteEntyId() {
    return suiteLibrary.values().stream()
        .map(Suite::getEntriesList)
        .flatMap(Collection::stream)
        .mapToLong(Suite.Entry::getId)
        .max().orElse(-1) + 1;
  }

  private static long nextScenarioId() {
    return scenarioLibrary.values().stream()
        .mapToLong(Scenario::getId)
        .max()
        .orElse(-1L) + 1;
  }

  public static void readFromFile() {
    String folderName = "eval/library/";
    ensureFolderExists(folderName);
    String fileName = folderName + FILE_NAME + ".dat";

    try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
      ScenarioProtos.EvalLibrary.Builder builder = ScenarioProtos.EvalLibrary.newBuilder();
      JsonFormat.parser().merge(reader, builder);

      builder.getScenariosList().forEach(scenario -> scenarioLibrary.put(scenario.getId(), scenario));
      builder.getSuitesList().forEach(suite -> suiteLibrary.put(suite.getId(), suite));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static final String FILE_NAME = "eval_library";
  private static final JsonFormat.Printer JSON_PRINTER = JsonFormat.printer()
      .includingDefaultValueFields()
      .preservingProtoFieldNames()
      .omittingInsignificantWhitespace();

  private static void writeToFile() {
    String folderName = "eval/library/";
    ensureFolderExists(folderName);
    String fileName = folderName + FILE_NAME + ".dat";

    ScenarioProtos.EvalLibrary evalLibrary = ScenarioProtos.EvalLibrary.newBuilder()
        .addAllScenarios(scenarioLibrary.values())
        .addAllSuites(suiteLibrary.values())
        .build();

    try (PrintWriter printWriter = new PrintWriter(new FileWriter(fileName))) {
      printWriter.append(JSON_PRINTER.print(evalLibrary));
    } catch (IOException e) {
      log2Console.error("Cannot write state", e);
    }
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private static void ensureFolderExists(String folderName) {
    new File(folderName).mkdirs();
  }

  private static class EvalRenderer extends Renderer {

    // The offset in render group index to not collide with the cars.
    private static final int EVAL_RENDERER_OFFSET = 65;

    // Non-static members.
    private RenderPacket previousPacket;

    private EvalRenderer() {
      super(EVAL_RENDERER_OFFSET);
    }

    private void initTick() {
      builder = new FlatBufferBuilder(1000);
    }

    private void sendData() {
      RenderPacket packet = doFinishPacket();
      if (!packet.equals(previousPacket)) {
        RLBotDll.sendRenderPacket(packet);
        previousPacket = packet;
      }
    }

    boolean isInitialized() {
      return builder != null;
    }

    public void render2dText(Color color, int x, int y, String text) {
      drawString2d(text, color, new Point(x, y), 2, 2);
    }
  }
}
