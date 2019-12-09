package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.main.Acg;
import com.eru.rlbot.bot.common.BotRenderer;
import com.eru.rlbot.bot.common.Pair;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import com.google.common.collect.ImmutableList;

import java.util.*;

public class TacticManager {

  private static final Map<Tactic.TacticType, Class<? extends Tactician>> DEFAULT_TACTICIAN_MAP = new HashMap<>();

  static {
    DEFAULT_TACTICIAN_MAP.put(Tactic.TacticType.AERIAL, AerialTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.TacticType.CATCH, CatchTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.TacticType.DEFEND, GoalLineTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.TacticType.DRIBBLE, DribbleTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.TacticType.FLIP, FlipTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.TacticType.FLICK, FlickTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.TacticType.HIT_BALL, RollingTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.TacticType.KICKOFF, KickoffTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.TacticType.PICK_UP, PickUpTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.TacticType.ROTATE, RotateTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.TacticType.SHADOW, ShadowTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.TacticType.STRIKE, TakeTheShotTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.TacticType.WALL_RIDE, SideWallTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.TacticType.WAVE_DASH, WaveDashTactician.class);
  }

  private final BotRenderer botRenderer;
  private LinkedList<Tactic> tacticList = new LinkedList<>();

  private final Acg bot;
  private final Set<Tactic> completedTactics = new HashSet<>();
  private Pair<Tactic.TacticType, Tactician> controllingTactician;

  public TacticManager(Acg bot) {
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
    setTactic(ImmutableList.of(tactic));
  }

  public void setTactic(ImmutableList<Tactic> tactics) {
    if (tactics.isEmpty() || (hasTactic() && tactics.get(0).equals(getTactic()))) {
      return;
    }

    controllingTactician = null;
    tacticList.clear();
    tacticList.addAll(tactics);
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

  public void changeTactic(Tactic tactic, Tactic.TacticType tacticType) {
    this.setTactic(tactic.withType(tacticType));
  }

  public void delegateTactic(Tactic tactic, Class<? extends Tactician> tactician) {
    this.controllingTactician = Pair.of(tactic.tacticType, newTactician(tactician));
  }

  public void preemptTactic(Tactic tactic) {
    this.tacticList.addFirst(tactic);
    this.controllingTactician = Pair.of(tactic.tacticType, getTactician(tactic));
  }

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
    return getTactician(getTactic());
  }

  private Tactician getTactician(Tactic tactic) {
    if (controllingTactician != null && controllingTactician.getFirst() == tactic.tacticType) {
      return controllingTactician.getSecond();
    } else {
      controllingTactician = null;
    }

    controllingTactician = Pair.of(tactic.tacticType, newTactician(DEFAULT_TACTICIAN_MAP.get(tactic.tacticType)));
    return controllingTactician.getSecond();
  }

  private Tactician newTactician(Class<? extends Tactician> t) {
    try {
      return t.getDeclaredConstructor(Acg.class, TacticManager.class)
          .newInstance(bot, this);
    } catch (Throwable e) {
      throw new IllegalStateException(String.format("Cannot create tactician %s", t), e);
    }
  }

  public void clearTactics() {
    tacticList.clear();
    controllingTactician = null;
  }

  public boolean isTacticLocked() {
    return controllingTactician != null && controllingTactician.getSecond().isLocked();
  }
}
