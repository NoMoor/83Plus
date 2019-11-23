package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.common.BotRenderer;
import com.eru.rlbot.bot.common.Pair;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

import java.util.*;

public class TacticManager {


  private static final Map<Tactic.Type, Class<? extends Tactician>> DEFAULT_TACTICIAN_MAP = new HashMap<>();

  static {
    DEFAULT_TACTICIAN_MAP.put(Tactic.Type.AERIAL, AerialTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.Type.CATCH, CatchTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.Type.DEFEND, GoalLineTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.Type.DRIBBLE, DribbleTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.Type.FRONT_FLIP, FlipTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.Type.FLICK, FlickTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.Type.HIT_BALL, RollingTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.Type.KICKOFF, KickoffTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.Type.PICK_UP, PickUpTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.Type.ROTATE, RotateTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.Type.SHADOW, ShadowTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.Type.STRIKE, TakeTheShotTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.Type.WALL_RIDE, SideWallTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.Type.WAVE_DASH, WaveDashTactician.class);
  }

  private final BotRenderer botRenderer;
  private LinkedList<Tactic> tacticList = new LinkedList<>();

  private final EruBot bot;
  private final Set<Tactic> completedTactics = new HashSet<>();
  private Pair<Tactic.Type, Tactician> controllingTactician;

  public TacticManager(EruBot bot) {
    this.bot = bot;
    this.botRenderer = BotRenderer.forBot(bot);
  }

  private Optional<Tactic> nextTactic() {
    return tacticList.isEmpty() ? Optional.empty() : Optional.of(tacticList.get(0));
  }

  public void addTactic(Tactic tactic) {
    if (!hasTactic() || !tactic.equals(getLastTactic())) { // TODO: Do something better here.
      tacticList.add(tactic);
    }
  }

  public void setTactic(Tactic tactic) {
    if (hasTactic() && tactic.equals(getTactic())) {
      return;
    }

    controllingTactician = null;
    tacticList.clear();
    tacticList.add(tactic);
  }

  public void execute(DataPacket input, ControlsOutput output) {
    botRenderer.setTactic(getTactic());
    botRenderer.setTactician(getTactician());

    getTactician().execute(input, output, getTactic());

    if (completedTactics.remove(getTactic()) && !tacticList.isEmpty()) {
      tacticList.pop();
    }
  }

  public void setTacticComplete(Tactic tactic) {
    this.completedTactics.add(tactic);
    this.controllingTactician = null;
  }

  public void changeTactic(Tactic tactic, Tactic.Type type) {
    this.setTactic(new Tactic(tactic.targetMoment.position, type));
  }

  public void delegateTactic(Tactic tactic, Class<? extends Tactician> tactician) {
    this.controllingTactician = Pair.of(tactic.type, newTactician(tactician));  }

  public boolean hasTactic() {
    return !tacticList.isEmpty();
  }

  private Tactic getTactic() {
    return tacticList.getFirst();
  }

  private Tactic getLastTactic() {
    return tacticList.getLast();
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
