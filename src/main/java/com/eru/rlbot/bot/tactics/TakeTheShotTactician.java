package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.common.RelativeUtils;
import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
import com.eru.rlbot.bot.optimizer.CarBallOptimizer;
import com.eru.rlbot.bot.optimizer.OptimizationResult;
import com.eru.rlbot.bot.path.Path;
import com.eru.rlbot.bot.path.PathPlanner;
import com.eru.rlbot.bot.prediction.BallPredictionUtil;
import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.Controls;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Relies on other low level tactical units to do the movement but this tactician is responsible for planning a shot on
 * goal.
 */
public class TakeTheShotTactician extends Tactician {

  private static final Logger logger = LogManager.getLogger("TakeTheShot");

  TakeTheShotTactician(ApolloGuidanceComputer bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  @Override
  public boolean isLocked() {
    return false;
  }

  private Path path;

  @Override
  public void internalExecute(DataPacket input, Controls output, Tactic tactic) {
    if (path == null || path.isOffCourse() || BallPredictionUtil.get(input.car).wasTouched()) {
      Optional<CarData> targetOptional = PathPlanner.closestStrike(input.car, tactic.subject);

      if (!targetOptional.isPresent()) {
        bot.botRenderer.setBranchInfo("Target not found");
        return;
      } else {
        bot.botRenderer.setBranchInfo("Target acquired");
      }

      CarData target = targetOptional.get();
      OptimizationResult optimalHit =
          CarBallOptimizer.xSpeed(tactic.subject, Goal.opponentGoal(input.car.team).center, target);

      path = PathPlanner.oneTurn(input.car, Moment.from(optimalHit.car));
      path.lockAndSegment();
      path.extendThroughBall();

      bot.botRenderer.renderHitBox(optimalHit.car);
      bot.botRenderer.setIntersectionTarget(target.position);
    }

    bot.botRenderer.renderPath(input, path);
    pathExecutor.executePath(input, output, path);

    if (output.getThrottle() < 0 && !output.holdBoost() && input.ball.velocity.magnitude() < .1) {
      BallData relativeBall = RelativeUtils.noseRelativeBall(input);
      logger.info("Slowing down! throttle: {} ballSpeed: {} ballDistance: {}", output.getThrottle(), input.ball.velocity.magnitude(), relativeBall.position);
    }
  }
}
