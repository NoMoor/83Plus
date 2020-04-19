package com.eru.rlbot.bot.path;

import com.eru.rlbot.bot.tactics.AerialTactician;
import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.common.Pair;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.LinkedList;
import java.util.List;

public class Plan {

  public final Path path;
  public final double traverseTime;
  public final double boostUsed;
  public final ImmutableList<ControlInput> throttleInputList;
  public final Tactic.TacticType type;
  public final Pair<AerialTactician.FlightPlan, AerialTactician.FlightLog> aerialPlan;

  public Plan(Builder builder) {
    Preconditions.checkArgument(builder.type != null, "All plans must have a type.");
    traverseTime = builder.time;
    path = builder.path;
    aerialPlan = builder.aerialPlan;
    boostUsed = builder.boostUsed;
    throttleInputList = ImmutableList.copyOf(builder.inputList);
    type = builder.type;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class ControlInput {

    public static final double COASTING = .02d;
    public static final double REVERSE = -1d;

    // Common control inputs.
    private static final ControlInput REVERSE_CONTROL = new ControlInput(false, REVERSE, false);
    private static final ControlInput COAST = new ControlInput(false, COASTING, false);
    private static final ControlInput JUMP = new ControlInput(false, 1, true);
    private static final ControlInput FULL_THROTTLE = new ControlInput(false, 1, false);
    private static final ControlInput BOOST = new ControlInput(true, 1, false);
    private static final ControlInput BOOST_JUMP = new ControlInput(true, 1, true);

    public static final ControlInput NO_INPUTS = new ControlInput(false, 0, false);

    public final boolean boost;
    public final double throttle;
    public final boolean jump;

    private ControlInput(boolean boost, double throttle, boolean jump) {
      this.boost = boost;
      this.throttle = throttle;
      this.jump = jump;
    }

    public static ControlInput create(boolean boost, double throttle, boolean jump) {
      if (boost) {
        return jump ? BOOST_JUMP : BOOST;
      } else if (jump) {
        return JUMP;
      } else if (throttle == 0) {
        return NO_INPUTS;
      } else if (throttle == COAST.throttle) {
        return COAST;
      } else if (throttle == REVERSE) {
        return REVERSE_CONTROL;
      } else if (throttle == 1) {
        return FULL_THROTTLE;
      } else {
        return new ControlInput(false, throttle, false);
      }
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
    private Pair<AerialTactician.FlightPlan, AerialTactician.FlightLog> aerialPlan;

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

    public Plan.Builder setAerialPlan(Pair<AerialTactician.FlightPlan, AerialTactician.FlightLog> plan) {
      this.aerialPlan = plan;
      return this;
    }

    public Plan build(double time) {
      this.time = time;
      return new Plan(this);
    }
  }
}
