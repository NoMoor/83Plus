package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
import com.eru.rlbot.bot.renderer.BotRenderer;
import com.eru.rlbot.common.Pair;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.Controls;
import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Keeps track of the various tacticians and calls the one corresponding to the current action.
 */
public class TacticManager {

  private static final Map<Tactic.TacticType, Class<? extends Tactician>> DEFAULT_TACTICIAN_MAP = new HashMap<>();

  static {
    DEFAULT_TACTICIAN_MAP.put(Tactic.TacticType.AERIAL, AerialTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.TacticType.CATCH, CatchTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.TacticType.DEMO, DemoTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.TacticType.DRIBBLE, DribbleTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.TacticType.FLIP, FlipTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.TacticType.FLICK, FlickTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.TacticType.GUARD, GuardianTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.TacticType.JUMP_FLIP, JumpFlipTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.TacticType.HIT_BALL, RollingTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.TacticType.KICKOFF, KickoffTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.TacticType.PICK_UP, PickUpTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.TacticType.ROTATE, RotateTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.TacticType.SHADOW, ShadowTactician.class);
    DEFAULT_TACTICIAN_MAP.put(Tactic.TacticType.STRIKE, TakeTheShotTactician.class);
  }

  private final Map<Tactic.TacticType, Tactician> tacticians = new HashMap<>();

  private final BotRenderer botRenderer;
  private LinkedList<Tactic> tacticList = new LinkedList<>();

  private final ApolloGuidanceComputer rocket;
  private final Set<Tactic> completedTactics = new HashSet<>();
  private Pair<Tactic.TacticType, Tactician> controllingTactician;

  public TacticManager(ApolloGuidanceComputer rocket) {
    this.rocket = rocket;
    this.botRenderer = BotRenderer.forBot(rocket);
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

  public void execute(DataPacket input, Controls output) {

    Tactician tactician = getTactician();
    Tactic tactic = getTactic();

    tactician.execute(input, output, tactic);

    botRenderer.setTactic(tactic);
    botRenderer.setTactician(tactician);

    if (completedTactics.remove(tactic) && !tacticList.isEmpty()) {
      tacticList.pop();
    }
  }

  public void setTacticComplete(Tactic tactic) {
    this.completedTactics.add(tactic);
    clearTactician();
  }

  public void changeTactic(Tactic tactic, Tactic.TacticType tacticType) {
    this.setTactic(tactic.withType(tacticType));
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
      clearTactician();
      controllingTactician = null;
    }

    Tactician tactician = tacticians.computeIfAbsent(tactic.tacticType, this::newTactician);

    controllingTactician = Pair.of(tactic.tacticType, tactician);
    return controllingTactician.getSecond();
  }

  private Tactician newTactician(Tactic.TacticType type) {
    return newTactician(DEFAULT_TACTICIAN_MAP.get(type));
  }

  private Tactician newTactician(Class<? extends Tactician> t) {
    try {
      return t.getDeclaredConstructor(ApolloGuidanceComputer.class, TacticManager.class)
          .newInstance(rocket, this);
    } catch (Throwable e) {
      throw new IllegalStateException(String.format("Cannot create tactician %s", t), e);
    }
  }

  public void clearTactics() {
    tacticList.clear();
    clearTactician();
  }

  private void clearTactician() {
    if (controllingTactician != null) {
      controllingTactician.getSecond().clearDelegate();
      controllingTactician = null;
    }
  }

  public boolean isTacticLocked() {
    return controllingTactician != null && controllingTactician.getSecond().isLocked();
  }
}
