package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.common.Path;
import com.eru.rlbot.bot.common.PathPlanner;
import com.eru.rlbot.bot.main.Agc;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;
import java.awt.Color;

public class RotateTactician extends Tactician {

  RotateTactician(Agc bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  public static boolean shouldRotateBack(DataPacket input) {
    double carToGoal = input.car.position.distance(Goal.ownGoal(input.car.team).center);
    double ballToGoal = input.ball.position.distance(Goal.ownGoal(input.car.team).center);

    return ballToGoal < carToGoal;
  }

  @Override
  void internalExecute(DataPacket input, ControlsOutput output, Tactic tactic) {
    usingPathPlanner(input, output, tactic);
  }

  private boolean locked;
  @Override
  public boolean isLocked() {
    return locked;
  }

  private void usingPathPlanner(DataPacket input, ControlsOutput output, Tactic tactic) {
    // TODO: Uses current ball position and future car position.
    Vector3 targetVelocity = input.ball.position.minus(tactic.subject.position).addX(.01).toMagnitude(1800);

    CarData targetCar = CarData.builder()
        .setPosition(tactic.subject.position)
        .setVelocity(targetVelocity)
        .setOrientation(Orientation.fromFlatVelocity(targetVelocity))
        .build();

    Path path = PathPlanner.planPath(input.car, targetCar);
    path.setTimed(false);
    bot.botRenderer.renderPath(input, path);
    bot.botRenderer.renderHitBox(Color.BLACK, targetCar);

    if (input.car.hasWheelContact) {
      pathExecutor.executePath(input, output, path);
    } else {
      Angles3.setControlsForFlatLanding(input.car, output);
    }
  }

  @Override
  public boolean allowDelegate() {
    return true;
  }
}
