package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
import com.eru.rlbot.bot.maneuver.WallHelper;
import com.eru.rlbot.bot.path.Path;
import com.eru.rlbot.bot.path.PathPlanner;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.Controls;

/**
 * Moves the ball back toward the given location.
 */
public class RotateTactician extends Tactician {

  RotateTactician(ApolloGuidanceComputer bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  public static boolean shouldRotateBack(DataPacket input) {
    double carToGoal = input.car.position.distance(Goal.ownGoal(input.car.team).center);
    double ballToGoal = input.ball.position.distance(Goal.ownGoal(input.car.team).center);

    return ballToGoal < carToGoal;
  }

  @Override
  void internalExecute(DataPacket input, Controls output, Tactic tactic) {
    usingPathPlanner(input, output, tactic);
  }

  private boolean locked;

  @Override
  public boolean isLocked() {
    return locked;
  }

  private void usingPathPlanner(DataPacket input, Controls output, Tactic tactic) {
    if (WallHelper.isOnWall(input.car)) {
      bot.botRenderer.setBranchInfo("Get off the wall");

      WallHelper.drive(input, output, input.ball.position);
      return;
    }

    Path path = PathPlanner.oneTurn(input.car, tactic.subject);

    if (path == null) {
      pathExecutor.executeSimplePath(input, output, tactic);
      return;
    } else {
      path.lockAndSegment(false);
    }

    bot.botRenderer.renderPath(input, path);

    if (input.car.hasWheelContact) {
      pathExecutor.executePath(input, output, path);
    } else {
      Angles3.setControlsForFlatLanding(input.car, output);
      output.withThrottle(1.0);
    }
  }

  @Override
  public boolean allowDelegate() {
    return true;
  }
}
