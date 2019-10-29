package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.common.BotRenderer;
import com.eru.rlbot.bot.common.Pair;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;

import java.awt.*;
import java.util.*;

public class TacticManager {


  private static final Map<Tactic.Type, Class<? extends Tactician>> DEFAULT_TACTICIAN_MAP = new HashMap<>();

  static {
    DEFAULT_TACTICIAN_MAP.put(Tactic.Type.CATCH, CatchTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.Type.DEFEND, BackupTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.Type.DRIBBLE, DribbleTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.Type.FRONT_FLIP, FlipTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.Type.FLICK, FlickTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.Type.HIT_BALL, RollingTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.Type.KICKOFF, KickoffTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.Type.PICK_UP, PickUpTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.Type.WALL_RIDE, SideWallTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.Type.WAVE_DASH, WaveDashTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.Type.ROTATE, RotateTactician.class);
  }

  private final Tactic defendTactic;
  private final Tactic dribbleTactic;
  private final Tactic defaultTactic;

  private final BotRenderer botRenderer;
  private LinkedList<Tactic> tacticList = new LinkedList<>();

  private final EruBot bot;
  private final Set<Tactic> completedTacticis = new HashSet<>();
  private Pair<Tactic.Type, Tactician> controllingTactician;

  public TacticManager(EruBot bot) {
    this.bot = bot;
    this.botRenderer = BotRenderer.forBot(bot);

    defendTactic = new Tactic(bot.ownGoal.center, Tactic.Type.DEFEND);
    dribbleTactic = new Tactic(bot.opponentsGoal.center, Tactic.Type.DRIBBLE);
    defaultTactic = dribbleTactic;
  }

  // TODO: Probably don't want to call this.
  public void updateTactics(DataPacket packet) {
    tacticList.forEach(tactic -> tactic.updateTactic(packet));
  }

  public Vector3 getNextTarget() {
    return nextTactic().getTarget();
  }

  private Tactic nextTactic() {
    return tacticList.isEmpty() ? defaultTactic : tacticList.get(0);
  }

  public void addTactic(Tactic tactic) {
    if (!tactic.equals(getLastTactic())) { // TODO: Do something better here.
      tacticList.add(tactic);
    }
  }

  public void setTactic(Tactic tactic) {
    tacticList.clear();
    tacticList.add(tactic);
  }

  public void execute(DataPacket input, ControlsOutput output) {
    getTactician().execute(input, output, getTactic());

    botRenderer.setTactic(getTactic());
    botRenderer.setTactician(getTactician());
    renderTactics(input.car);

    if (completedTacticis.remove(getTactic()) && !tacticList.isEmpty()) {
      tacticList.pop();
    }
  }

  private void renderTactics(CarData carData) {
    Vector3 previousTarget = carData.position;

    if (!tacticList.isEmpty()) {
      for (int i = 0; i < tacticList.size(); i++) {
        Vector3 nextTarget = tacticList.get(i).target.position;
        bot.botRenderer.render3DLine(i == 0 ? Color.green : Color.ORANGE, previousTarget, nextTarget);
        previousTarget = nextTarget;
      }
    } else {
      Vector3 nextTarget = getNextTarget();
      bot.botRenderer.render3DLine(Color.green, previousTarget, nextTarget);
    }
  }

  public void setTacticComplete(Tactic tactic) {
    this.completedTacticis.add(tactic);
    this.controllingTactician = null;
  }

  public void changeTactic(Tactic tactic, Tactic.Type type) {
    this.setTactic(new Tactic(tactic.target.position, type));
  }

  public void delegateTactic(Tactic tactic, Class<? extends Tactician> tactician) {
    this.controllingTactician = Pair.of(tactic.type, newTactician(tactician));  }

  private Tactic getTactic() {
    return tacticList.isEmpty() ? defaultTactic : tacticList.getFirst();
  }

  private Tactic getLastTactic() {
    return tacticList.isEmpty() ? defaultTactic : tacticList.getLast();
  }

  private Tactician getTactician() {
    Tactic tactic = getTactic();

    if (controllingTactician != null && controllingTactician.getFirst() == tactic.type) {
      return controllingTactician.getSecond();
    } else {
      controllingTactician = null;
    }

    controllingTactician = Pair.of(tactic.type, newTactician(DEFAULT_TACTICIAN_MAP.get(tactic.type)));
    return controllingTactician.getSecond();
  }

  private Tactician newTactician(Class<? extends Tactician> t) {
    try {
      return t.getDeclaredConstructor(EruBot.class, TacticManager.class)
          .newInstance(bot, this);
    } catch (Throwable e) {
      throw new IllegalStateException(String.format("Cannot create tactician %s", t), e);
    }
  }

  public void clearTactics() {
    tacticList.clear();
  }

  public boolean isTacticLocked() {
    return controllingTactician != null && controllingTactician.getSecond().isLocked();
  }
}
