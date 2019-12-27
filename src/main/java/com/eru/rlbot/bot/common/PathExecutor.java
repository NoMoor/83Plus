package com.eru.rlbot.bot.common;

import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.bot.tactics.Tactician;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

public class PathExecutor {

  private final Tactician tactician;

  private PathExecutor(Tactician tactician) {
    this.tactician = tactician;
  }

  public static PathExecutor forTactician(Tactician tactician) {
    return new PathExecutor(tactician);
  }

  // TODO: Add in minor path deviation for boosts.
  // TODO: Generalize this and add in flips.
  public static final float TIME_BUFFER = .2f;

  public void executePath(DataPacket input, ControlsOutput output, Path path) {
    double traverseTime = path.minimumTraverseTime();
    double currentTime = input.car.elapsedSeconds;
    double pathEndTime = path.getEndTime();

    if (traverseTime + currentTime + TIME_BUFFER < pathEndTime) {
      BotRenderer.forIndex(input.car.playerIndex)
          .setBranchInfo("Pause");
      output.withThrottle(.02);
      return;
    }
    BotRenderer.forIndex(input.car.playerIndex)
        .setBranchInfo("Offset: %f", pathEndTime - (traverseTime + currentTime));

    Path.Segment nextSegment = path.getSegment(input);

    double correctionAngle = Angles.flatCorrectionAngle(input.car, nextSegment.end);
    double minTurnRadius = Constants.radius(input.car.velocity.magnitude());

    Circle turn = Paths.turnRadius(input.car, nextSegment.end);

    boolean insideTurnRadius = turn.center.distance(nextSegment.end) < minTurnRadius;
    boolean hardTurn = turn.center.distance(nextSegment.end) < minTurnRadius * 1.15;

    boolean isFacingAwayFromTarget = input.car.orientation.getNoseVector()
        .dot(nextSegment.end.minus(input.car.position)) < 0;

    boolean canGoFaster = path.canGoFaster(input.car.groundSpeed);
    boolean breakNow = path.breakNow(input.car);

    output
        .withSteer(hardTurn ? Math.signum(correctionAngle) : correctionAngle)
        .withThrottle(breakNow ? -1 : (insideTurnRadius || !canGoFaster) ? 0.02 : 1)
        .withBoost(!insideTurnRadius && !isFacingAwayFromTarget && canGoFaster && !breakNow)
        .withSlide(isFacingAwayFromTarget);

    if (!input.car.hasWheelContact) {
      Angles3.setControlsForFlatLanding(input.car, output);
    }

    // This is not a timed path. Go ahead and use advanced mechanics.
    if (!path.isTimed()) {
      double distanceToTarget = input.car.position.distance(nextSegment.end);

      // TODO: Build this timing into the calculation.
      if (((distanceToTarget > input.car.groundSpeed * 2 && nextSegment.end.z < 150) || distanceToTarget > input.car.groundSpeed * 2.5) // Distance
          && ((input.car.groundSpeed > 1300 && input.car.boost < 20) || input.car.groundSpeed > 1500) // Speed
          && !input.car.isSupersonic // Excess Speed
          && Math.abs(correctionAngle) < .25) { // Angle

        tactician.getTacticManager().preemptTactic(Tactic.builder()
            .setSubject(nextSegment.end)
            .setTacticType(Tactic.TacticType.FLIP)
            .build());
      }
    }
  }
}
