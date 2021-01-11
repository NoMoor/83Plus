package com.eru.rlbot.testing.eval;

import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.common.ScenarioProtos;
import com.google.protobuf.util.JsonFormat;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
import rlbot.gamestate.GameState;

public class EvalGui {

  private static final Logger log2Console = LogManager.getLogger("EvalGuiConsole");

  private static final List<ScenarioProtos.Scenario> scenarioLibrary = new ArrayList<>();

  private static DefaultTableModel tableModel;

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

  private static JTable scenarioTable;
  private static JTextField nameField;
  private static JButton startButton;

  // Table
  // Input elements
  // Read from file
  // Write to file
  public static void show() {
    JFrame frame = new JFrame("Eval");
    frame.setSize(500, 500);
    frame.setVisible(true);

    scenarioTable = new JTable();
    tableModel = new DefaultTableModel() {
      @Override
      public int getRowCount() {
        return scenarioLibrary.size();
      }

      @Override
      public String getValueAt(int row, int column) {
        ScenarioProtos.Scenario scenario = scenarioLibrary.get(row);

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

    tableModel.addColumn("Scenario #");
    tableModel.addColumn("Scenario Name");
    tableModel.addColumn("Ball Info");
    scenarioTable.setModel(tableModel);
    scenarioTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    scenarioTable.getSelectionModel().addListSelectionListener(EvalGui::updatePickers);

    JScrollPane tableScrollPane = new JScrollPane(scenarioTable);
    tableScrollPane.setPreferredSize(new Dimension(400, 200));

    JPanel panel = new JPanel();

    startButton = new JButton("Start");
    startButton.addActionListener(EvalGui::setState);
    startButton.setEnabled(false);

    panel.add(startButton);
    panel.add(tableScrollPane);
    panel.add(createSetterPanel());

    frame.add(panel);
  }

  private static void setState(ActionEvent actionEvent) {
    int selection = scenarioTable.getSelectionModel().getAnchorSelectionIndex();
    if (selection == -1) {
      return;
    }

    ScenarioProtos.Scenario scenario = scenarioLibrary.get(selection);

    // TODO: Have a timeout and title rendering.
    RLBotDll.setGameState(new GameState()
        .withBallState(Utils.toDesired(scenario.getBall()))
        .withCarState(0, Utils.toDesired(scenario.getCar(0)))
        .buildPacket());
  }

  private static void updatePickers(ListSelectionEvent listSelectionEvent) {
    if (listSelectionEvent.getValueIsAdjusting()) {
      return;
    }

    int selection = scenarioTable.getSelectionModel().getAnchorSelectionIndex();
    startButton.setEnabled(selection != -1);
    if (selection == -1) {
      return;
    }

    ScenarioProtos.Scenario scenario = scenarioLibrary.get(selection);
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

    JLabel crolllabel = new JLabel("Car vZ");
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

    JButton createButton = new JButton("Create");
    createButton.addActionListener(e -> {
      scenarioLibrary.add(fromFields()
          .setId(nextScenarioId())
          .setCreated(System.currentTimeMillis())
          .build());
      tableModel.fireTableDataChanged();
      writeToFile();
    });

    JButton updateButton = new JButton("Update");
    updateButton.addActionListener(e -> {
      int selectedRow = scenarioTable.getSelectedRow();

      if (selectedRow != -1) {
        ScenarioProtos.Scenario oldScenario = scenarioLibrary.remove(selectedRow);
        ScenarioProtos.Scenario scenario = fromFields().setId(oldScenario.getId())
            .setUpdated(System.currentTimeMillis())
            .build();
        scenarioLibrary.add(selectedRow, scenario);
        tableModel.fireTableDataChanged();
        scenarioTable.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
        writeToFile();
      }
    });

    creationPanel.add(createButton);
    creationPanel.add(updateButton);

    return creationPanel;
  }

  private static ScenarioProtos.Scenario.Builder fromFields() {
    ScenarioProtos.Scenario.BallState data = ScenarioProtos.Scenario.BallState.newBuilder()
        .addPos(bxModel.getNumber().floatValue())
        .addPos(byModel.getNumber().floatValue())
        .addPos(bzModel.getNumber().floatValue())
        .addVel(bvxModel.getNumber().floatValue())
        .addVel(bvyModel.getNumber().floatValue())
        .addVel(bvzModel.getNumber().floatValue())
        .build();

    ScenarioProtos.Scenario.CarState car = ScenarioProtos.Scenario.CarState.newBuilder()
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

    return ScenarioProtos.Scenario.newBuilder()
        .setId(nextScenarioId())
        .setName(nameField.getText().equals("") ? "Scenario " + scenarioLibrary.size() : nameField.getText())
        .setBall(data)
        .addCar(car);
  }

  private static long nextScenarioId() {
    return scenarioLibrary.stream()
        .max(Comparator.comparing(ScenarioProtos.Scenario::getId))
        .map(ScenarioProtos.Scenario::getId)
        .orElse(-1L) + 1;
  }

  public static void readFromFile() {
    String folderName = "eval/library/";
    ensureFolderExists(folderName);
    String fileName = folderName + FILE_NAME + ".dat";

    try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
      ScenarioProtos.EvalLibrary.Builder builder = ScenarioProtos.EvalLibrary.newBuilder();
      JsonFormat.parser().merge(reader, builder);
      scenarioLibrary.addAll(builder.getScenariosList());
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
        .addAllScenarios(scenarioLibrary)
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
}
