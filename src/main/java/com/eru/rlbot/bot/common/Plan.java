package com.eru.rlbot.bot.common;

import com.google.common.collect.ImmutableList;
import java.util.LinkedList;
import java.util.List;

public class Plan {

  public final Path path;
  public final double traverseTime;
  public final double boostUsed;
  public final ImmutableList<ThrottleInput> throttleInputList;

  public Plan(Builder builder) {
    traverseTime = builder.time;
    path = builder.path;
    boostUsed = builder.boostUsed;
    throttleInputList = ImmutableList.copyOf(builder.inputList);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class ThrottleInput {
    public final boolean boost;
    public final double throttle;

    private ThrottleInput(boolean boost, double throttle) {
      this.boost = boost;
      this.throttle = throttle;
    }

    public static ThrottleInput create(boolean boost, double throttle) {
      return new ThrottleInput(boost, boost ? 1.0 : throttle);
    }

    @Override
    public String toString() {
      return "b:" + boost + "t:" + throttle;
    }
  }

  public static class Builder {
    private List<ThrottleInput> inputList = new LinkedList<>();
    private Path path;
    private double time;
    private double boostUsed;

    Builder setPath(Path path) {
      this.path = path;
      return this;
    }

    Builder addThrottleInput(boolean boost, double throttle) {
      inputList.add(ThrottleInput.create(boost, throttle));
      return this;
    }

    public Plan.Builder setBoostUsed(double value) {
      this.boostUsed = value;
      return this;
    }

    Plan build(double time) {
      this.time = time;
      return new Plan(this);
    }
  }
}
