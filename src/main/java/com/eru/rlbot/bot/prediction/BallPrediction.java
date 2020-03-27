package com.eru.rlbot.bot.prediction;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.eru.rlbot.bot.common.Teams;
import com.eru.rlbot.bot.path.Path;
import com.eru.rlbot.bot.path.Plan;
import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * For each prediction slice, this keeps track of what analysis has been done.
 */
public class BallPrediction {

  private final HashMap<Integer, Potential> botPotential = new HashMap<>();

  public final BallData ball;

  public BallPrediction(BallData ball) {
    this.ball = ball;
  }

  public Tactic.TacticType getTactic() {
    return ball.position.z > 300 ? Tactic.TacticType.AERIAL : Tactic.TacticType.STRIKE;
  }

  public boolean isHittableBySomeone() {
    return botPotential.values().stream().anyMatch(Potential::isHittable);
  }

  public boolean isHittable(CarData car) {
    return getPotential(car.serialNumber).isHittable();
  }

  private Potential getPotential(int serialNumber) {
    return botPotential.computeIfAbsent(serialNumber, Potential::new);
  }

  public Potential forCar(int serialNumber) {
    return getPotential(serialNumber);
  }

  public ImmutableList<Integer> ableToReach() {
    return botPotential.values().stream()
        .filter(Potential::isHittable)
        .map(potential -> potential.index)
        .collect(toImmutableList());
  }

  public ImmutableList<Integer> ableToReachTeams() {
    return botPotential.values().stream()
        .filter(Potential::isHittable)
        .map(potential -> Teams.getTeamForBot(potential.index))
        .collect(toImmutableList());
  }

  public boolean isHittableByTeam(int team) {
    return botPotential.values().stream()
        .filter(Potential::isHittable)
        .anyMatch(potential -> Teams.getTeamForBot(potential.index) == team);
  }

  public static class Potential {

    public final int index;
    private final List<Plan> plans = new ArrayList<>();

    public Potential(int index) {
      this.index = index;
    }

    public boolean isHittable() {
      return !plans.isEmpty();
    }

    public boolean hasPlan() {
      return plans.size() > 1;
    }

    public void addPlan(Plan plan) {
      plans.add(plan);
    }

    public Plan getPlan() {
      return plans.isEmpty() ? null : plans.get(0);
    }

    public Path getPath() {
      return plans.isEmpty() ? null : plans.get(0).path;
    }
  }
}
