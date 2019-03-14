package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.Bot;
import rlbot.manager.BotLoopRenderer;
import rlbot.render.Renderer;

import java.awt.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class TacticManager {

  private LinkedList<Tactic> tacticList = new LinkedList<>();
  private Map<Tactic.Type, Tactician> TACTICIAN_MAP = new HashMap<>();
  private static Tactic DEFAULT_TACTIC = new Tactic(Goal.ownGoal(0).center, Tactic.Type.DEFEND);

  private final Bot bot;

  public TacticManager(Bot bot) {
    this.bot = bot;

    // TODO(ahatfield): Add here
    TACTICIAN_MAP.put(Tactic.Type.FRONT_FLIP, new FlipTactician());
    TACTICIAN_MAP.put(Tactic.Type.WALL_RIDE, new SideWallTactician());
    TACTICIAN_MAP.put(Tactic.Type.HIT_BALL, new RollingTactician(bot));
    TACTICIAN_MAP.put(Tactic.Type.DEFEND, new BackupTactician());
  }

  public void updateTactics(DataPacket packet) {
    tacticList.forEach(tactic -> tactic.updateTactic(packet));
  }

  public Vector3 getNextTarget() {
    Tactic tactic = tacticList.isEmpty() ? DEFAULT_TACTIC : tacticList.get(0);
    return tactic.getTarget();
  }

  public void addTactic(Tactic tactic) {
    tacticList.add(tactic);
  }

  // TODO: Tactics should probably indicate when they are fulfilled.
  public Tactic tacticFulfilled() {
    return tacticList.pop();
  }

  public void setTactic(Tactic tactic) {
    tacticList.clear();
    tacticList.add(tactic);
  }

  public void execute(DataPacket input, ControlsOutput output) {
    Tactic nextTactic = tacticList.isEmpty() ? DEFAULT_TACTIC : tacticList.get(0);
    if (TACTICIAN_MAP.containsKey(nextTactic.type)) {
      TACTICIAN_MAP.get(nextTactic.type).execute(output, input, nextTactic);
    } else {
      System.out.println("No tactician found for tactic " + nextTactic.type);
      System.out.println("map: " + TACTICIAN_MAP.toString());
    }
  }

  public void renderTactic(CarData carData) {
    Renderer renderer = BotLoopRenderer.forBotLoop(bot);

    // Draw a line from the car to the ball
    renderer.drawLine3d(Color.LIGHT_GRAY, carData.position, getNextTarget());
  }

  public void clearTactics() {
    tacticList.clear();
  }
}
