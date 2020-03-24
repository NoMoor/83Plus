package com.eru.rlbot.bot.strats;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.eru.rlbot.bot.common.Teams;
import com.eru.rlbot.bot.renderer.BotRenderer;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.google.common.collect.ImmutableList;
import java.util.concurrent.ConcurrentHashMap;

public class Rotations {

  private static final ConcurrentHashMap<Integer, Rotations> PER_CAR = new ConcurrentHashMap<>();

  private final int team;
  private final int teamSize;
  private final int playerIndex;

  private CarData one;
  private CarData two;
  private CarData three;
  private CarData four;
  private CarData priority;
  private boolean teammateCommitted;

  private Rotations(int playerIndex, int team) {
    this.playerIndex = playerIndex;
    this.team = team;
    this.teamSize = Teams.getTeamSize(team);
  }

  public static Rotations get(DataPacket input) {
    return get(input.car);
  }

  public static Rotations get(CarData car) {
    return get(car.serialNumber, car.team);
  }

  private static Rotations get(int serialNumber, int teamIndex) {
    return PER_CAR.computeIfAbsent(serialNumber, n -> new Rotations(serialNumber, teamIndex));
  }

  public static void render(DataPacket input) {
    get(input).renderRotation();
  }

  public void track(DataPacket input) {
    ImmutableList<CarData> allies = input.allCars.stream()
        .filter(car -> car.team == team)
        .collect(toImmutableList());

    one = allies.get(0);
    if (teamSize > 1) {
      two = allies.get(1);
    }
    if (teamSize > 2) {
      three = allies.get(2);
    }
    if (teamSize > 3) {
      four = allies.get(3);
    }

    // TODO: Priority is defined as the next person to hit the ball. This is different from the rotation order which is
    // closer to a defense order.
    priority = input.car;

    // TODO: Determine if one of our teammates is already aerialing at the ball.
    teammateCommitted = false;
  }

  private void renderRotation() {
    BotRenderer botRenderer = BotRenderer.forIndex(playerIndex);
    ImmutableList<CarData> rotation = asList();
    for (int i = 0; i < rotation.size(); i++) {
      botRenderer.renderRotation(rotation.get(i), i + 1);
    }
  }

  public boolean isLastManBack() {
    switch (teamSize) {
      case 1:
        return one.serialNumber == playerIndex;
      case 2:
        return two.serialNumber == playerIndex;
      case 3:
        return three.serialNumber == playerIndex;
      case 4:
        return four.serialNumber == playerIndex;
    }
    throw new IllegalStateException("Cannot determine last man back for team of size " + teamSize);
  }

  private ImmutableList<CarData> asList() {
    ImmutableList.Builder<CarData> teamBuilder = ImmutableList.builder();
    teamBuilder.add(one);
    if (teamSize > 1) {
      teamBuilder.add(two);
    }
    if (teamSize > 2) {
      teamBuilder.add(three);
    }
    if (teamSize > 3) {
      teamBuilder.add(four);
    }
    return teamBuilder.build();
  }

  public boolean hasPriority() {
    return priority == null || priority.serialNumber == playerIndex;
  }

  public boolean teammateCommitted() {
    return teammateCommitted;
  }
}
