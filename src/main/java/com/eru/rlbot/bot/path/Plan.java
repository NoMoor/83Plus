package com.eru.rlbot.bot.path;

import com.eru.rlbot.bot.tactics.Tactic;
import com.google.common.collect.ImmutableList;
import java.util.LinkedList;
import java.util.List;

public class Plan {

  public final Path path;
  public final double traverseTime;
  public final double boostUsed;
  public final ImmutableList<ControlInput> throttleInputList;
  public final Tactic.TacticType type;

  public Plan(Builder builder) {
    traverseTime = builder.time;
    path = builder.path;
    boostUsed = builder.boostUsed;
    throttleInputList = ImmutableList.copyOf(builder.inputList);
    type = builder.type;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class ControlInput {

    public static final ControlInput NO_INPUTS = create(false, 0, false);

    public final boolean boost;
    public final double throttle;
    public final boolean jump;

    private ControlInput(boolean boost, double throttle, boolean jump) {
      this.boost = boost;
      this.throttle = throttle;
      this.jump = jump;
    }

    public static ControlInput create(boolean boost, double throttle, boolean jump) {
      return new ControlInput(boost, boost ? 1.0 : throttle, jump);
    }

    @Override
    public String toString() {
      return "b:" + boost + " t:" + throttle + " j:" + jump;
    }
  }

  public static class Builder {
    private List<ControlInput> inputList = new LinkedList<>();
    private Path path;
    private double time;
    private double boostUsed;
    private Tactic.TacticType type;

    public Builder setPath(Path path) {
      this.path = path;
      return this;
    }

    public Builder addThrottleInput(boolean boost, double throttle) {
      inputList.add(ControlInput.create(boost, throttle, false));
      return this;
    }

    public Builder addJumpInput(boolean jump) {
      inputList.add(ControlInput.create(false, 0, jump));
      return this;
    }

    public Plan.Builder setBoostUsed(double value) {
      this.boostUsed = value;
      return this;
    }

    public Plan.Builder setTacticType(Tactic.TacticType type) {
      this.type = type;
      return this;
    }

    public Plan build(double time) {
      this.time = time;
      return new Plan(this);
    }
  }
}
