package com.eru.rlbot.testing.eval;

import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.vector.Vector3;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
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
import rlbot.cppinterop.RLBotDll;
import rlbot.gamestate.BallState;
import rlbot.gamestate.CarState;
import rlbot.gamestate.GameState;
import rlbot.gamestate.PhysicsState;

public class EvalGui {

  private static final List<Scenario> scenarios = new ArrayList<>();
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
        return scenarios.size();
      }

      @Override
      public Object getValueAt(int row, int column) {
        Scenario scenario = scenarios.get(row);

        switch (column) {
          case 0:
            return scenario.getName();
          case 1:
          default:
            return scenario.getBall().toCsv();
        }
      }
    };

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

    Scenario scenario = scenarios.get(selection);

    // TODO: Have a timeout and title rendering.
    RLBotDll.setGameState(new GameState()
        .withBallState(new BallState()
            .withPhysics(new PhysicsState()
                .withLocation(scenario.getBall().position.toDesired())
                .withVelocity(scenario.getBall().velocity.toDesired())))
        .withCarState(0, new CarState()
            .withBoostAmount((float) scenario.getCar().boost)
            .withPhysics(new PhysicsState()
                .withLocation(scenario.getCar().position.toDesired())
                .withVelocity(scenario.getCar().velocity.toDesired())
                .withAngularVelocity(scenario.getCar().angularVelocity.toDesired())
                .withRotation(scenario.getCar().orientation.toEuclidianVector())))
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

    Scenario scenario = scenarios.get(selection);
    nameField.setText(scenario.getName());
    bxModel.setValue(scenario.getBall().position.x);
    byModel.setValue(scenario.getBall().position.y);
    bzModel.setValue(scenario.getBall().position.z);
    bvxModel.setValue(scenario.getBall().velocity.x);
    bvyModel.setValue(scenario.getBall().velocity.y);
    bvzModel.setValue(scenario.getBall().velocity.z);

    cxModel.setValue(scenario.getCar().position.x);
    cyModel.setValue(scenario.getCar().position.y);
    czModel.setValue(scenario.getCar().position.z);
    cvxModel.setValue(scenario.getCar().velocity.x);
    cvyModel.setValue(scenario.getCar().velocity.y);
    cvzModel.setValue(scenario.getCar().velocity.z);
    cPitchModel.setValue(scenario.getCar().orientation.toEuclidianVector().pitch);
    cYawModel.setValue(scenario.getCar().orientation.toEuclidianVector().yaw);
    cRollModel.setValue(scenario.getCar().orientation.toEuclidianVector().roll);
    boostModel.setValue(scenario.getCar().boost);
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
    bzModel = new SpinnerNumberModel(0, 0 + Constants.BALL_RADIUS, Constants.FIELD_HEIGHT - Constants.BALL_RADIUS, 20);
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
    czModel = new SpinnerNumberModel(0, Constants.CAR_AT_REST, Constants.FIELD_HEIGHT, 20);
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
      BallData data = BallData.builder()
          .setPosition(Vector3.of(bxModel.getNumber().doubleValue(), byModel.getNumber().doubleValue(), bzModel.getNumber().doubleValue()))
          .setVelocity(Vector3.of(bvxModel.getNumber().doubleValue(), bvyModel.getNumber().doubleValue(), bvzModel.getNumber().doubleValue()))
          .build();

      CarData car = CarData.builder()
          .setPosition(Vector3.of(cxModel.getNumber().doubleValue(), cyModel.getNumber().doubleValue(), czModel.getNumber().doubleValue()))
          .setVelocity(Vector3.of(cvxModel.getNumber().doubleValue(), cvyModel.getNumber().doubleValue(), cvzModel.getNumber().doubleValue()))
          .setOrientation(Orientation.convert(cPitchModel.getNumber().doubleValue(), cYawModel.getNumber().doubleValue(), cRollModel.getNumber().doubleValue()))
          .setBoost(boostModel.getNumber().doubleValue())
          .setAngularVelocity(Vector3.zero())
          .build();

      Scenario scenario = new Scenario(nameField.getText().equals("") ? "Scenario " + scenarios.size() : nameField.getText(), data, car);
      scenarios.add(scenario);
      tableModel.fireTableDataChanged();
    });

    JButton updateButton = new JButton("Update");
    updateButton.addActionListener(e -> {
      BallData data = BallData.builder()
          .setPosition(Vector3.of(bxModel.getNumber().doubleValue(), byModel.getNumber().doubleValue(), bzModel.getNumber().doubleValue()))
          .setVelocity(Vector3.of(bvxModel.getNumber().doubleValue(), bvyModel.getNumber().doubleValue(), bvzModel.getNumber().doubleValue()))
          .build();

      CarData car = CarData.builder()
          .setPosition(Vector3.of(cxModel.getNumber().doubleValue(), cyModel.getNumber().doubleValue(), czModel.getNumber().doubleValue()))
          .setVelocity(Vector3.of(cvxModel.getNumber().doubleValue(), cvyModel.getNumber().doubleValue(), cvzModel.getNumber().doubleValue()))
          .setOrientation(Orientation.convert(cPitchModel.getNumber().doubleValue(), cYawModel.getNumber().doubleValue(), cRollModel.getNumber().doubleValue()))
          .setBoost(boostModel.getNumber().doubleValue())
          .setAngularVelocity(Vector3.zero())
          .build();

      int selectedRow = scenarioTable.getSelectedRow();

      if (selectedRow != -1) {
        Scenario oldScenario = scenarios.remove(selectedRow);
        Scenario scenario = new Scenario(oldScenario.getName().endsWith(" (updated)")
            ? oldScenario.getName()
            : (oldScenario.getName() + " (updated)"), data, car);
        scenarios.add(selectedRow, scenario);
        tableModel.fireTableDataChanged();
        scenarioTable.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
      }
    });

    creationPanel.add(createButton);
    creationPanel.add(updateButton);

    return creationPanel;
  }
}
