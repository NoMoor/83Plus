package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.common.BotRenderer;
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

  private final BotRenderer botRenderer;
  private LinkedList<Tactic> tacticList = new LinkedList<>();
  private Map<Tactic.Type, Tactician> TACTICIAN_MAP = new HashMap<>();

  private static Tactic DEFEND_TACTIC = new Tactic(Goal.ownGoal(0).center, Tactic.Type.DEFEND);
  private static Tactic DRIBBLE_TACTIC = new Tactic(Goal.ownGoal(0).center, Tactic.Type.DRIBBLE);
  private static Tactic DEFAULT_TACTIC = DRIBBLE_TACTIC;

  private final Bot bot;

  // Mostly here to draw nice lines.
  private Vector3 endGoal;

  public TacticManager(Bot bot) {
    this.bot = bot;
    this.botRenderer = BotRenderer.forBot(bot);

    TACTICIAN_MAP.put(Tactic.Type.FRONT_FLIP, new FlipTactician());
    TACTICIAN_MAP.put(Tactic.Type.WALL_RIDE, new SideWallTactician());
    TACTICIAN_MAP.put(Tactic.Type.HIT_BALL, new RollingTactician(bot));
    TACTICIAN_MAP.put(Tactic.Type.DEFEND, new BackupTactician());
    TACTICIAN_MAP.put(Tactic.Type.DRIBBLE, new DribbleTactician(bot));
    TACTICIAN_MAP.put(Tactic.Type.KICKOFF, new KickoffTactician(bot));
  }

  // TODO: Probably don't want to call this.
  public void updateTactics(DataPacket packet) {
    tacticList.forEach(tactic -> tactic.updateTactic(packet));
  }

  public Vector3 getNextTarget() {
    return nextTactic().getTarget();
  }

  private Tactic nextTactic() {
    return tacticList.isEmpty() ? DEFAULT_TACTIC : tacticList.get(0);
  }

  public void addTactic(Tactic tactic) {
    tacticList.add(tactic);
  }

  public void setTactic(Tactic tactic) {
    tacticList.clear();
    tacticList.add(tactic);
  }

  public void execute(DataPacket input, ControlsOutput output) {
    if (nextTactic().isDone(input)) {
      tacticList.pop();
    }

    getTactician().execute(output, input, getTactic());

    boolean result = true;

    botRenderer.setTactic(getTactic());

    renderTactics(input.car);

    if (result && !tacticList.isEmpty()) {
      tacticList.removeFirst();
    }
  }

  private void renderTactics(CarData carData) {
    Renderer renderer = BotLoopRenderer.forBotLoop(bot);

    Vector3 previousTarget = carData.position;

    if (!tacticList.isEmpty()) {
      for (int i = 0; i < tacticList.size(); i++) {
        Vector3 nextTarget = tacticList.get(i).target.position;
        renderer.drawLine3d(i == 0 ? Color.green : Color.ORANGE, previousTarget, nextTarget);
        previousTarget = nextTarget;
      }
    } else {
      Vector3 nextTarget = getNextTarget();
      renderer.drawLine3d(Color.green, previousTarget, nextTarget);
      previousTarget = nextTarget;
    }

    // Render end goal
    if (endGoal != null) {
      renderer.drawLine3d(Color.red, previousTarget, endGoal);
    }

//    botRenderer.addDebugText(String.format("Tactician: %s", getTactician() == null ? "None" : getTactician().getClass().getSimpleName()), Color.CYAN);
//    botRenderer.addDebugText(String.format("Tactic: %s", getTactic() == null ? "None" : getTactic()), Color.CYAN);
    // Update moment time (rel).
    //botRenderer.addDebugText(String.format("Tactic Time (rel): %s", renderTime(nextTactic().target.time - carData.elapsedSeconds)), Color.CYAN);
  }

  private Tactic getTactic() {
    return tacticList.isEmpty() ? DEFAULT_TACTIC : tacticList.get(0);
  }

  private Tactician getTactician() {
    return TACTICIAN_MAP.get(getTactic().type);
  }

  private String renderTime(float time) {
    int m = (int) (time / 60);
    int s = (int) (time % 60);

    if (m == 0) {
      return String.format("%02.2f", time);
    }

    // Update time til moment.
    return String.format("%02d:%02d", m, s);
  }

  public void clearTactics() {
    tacticList.clear();
  }

  public void setEndGoal(Vector3 targetLocation) {
    this.endGoal = targetLocation;
  }
}
