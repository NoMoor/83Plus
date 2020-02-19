package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.common.Accels;
import com.eru.rlbot.bot.common.NormalUtils;
import com.eru.rlbot.bot.common.Path;
import com.eru.rlbot.bot.common.PathPlanner;
import com.eru.rlbot.bot.main.Agc;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// Relies on other low level tactical units to do the movement but this tactician is responsible for planning a shot on
// goal.
public class TakeTheShotTactician extends Tactician {

  private static final Logger logger = LogManager.getLogger("TakeTheShot");

  TakeTheShotTactician(Agc bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  public static boolean takeTheShot(DataPacket input) {
    if (input.allCars.size() == 1) {
      return true;
    }

    BallData relativeBallData = NormalUtils.noseRelativeBall(input);

    // TODO: Replace this with another time to ball equation.
    double timeToBall = timeToBall(relativeBallData, input.car);

    // Am the closest car to the ball.
    for (int i = 0 ; i < input.allCars.size() ; i++) {
      CarData nextCar = input.allCars.get(i);
      if (nextCar == input.car) {
        continue;
      }

      BallData relativeBallDataI = NormalUtils.noseRelativeBall(input, i);

      double oppTimeToBall = timeToBall(relativeBallDataI, nextCar);

      if (oppTimeToBall < timeToBall + .25) {
        return false;
      }
    }

    return true;
  }

  private static double timeToBall(BallData relativeBall, CarData car) {
    return car.boost > 40
        ? Accels.minTimeToDistance(car, relativeBall.position.flatten().norm()).time
        : Accels.timeToDistance(car.velocity.flatten().norm(), relativeBall.position.flatten().norm()).time;
  }

  private Path path;

  @Override
  public boolean isLocked() {
    return false;
  }

  @Override
  public void execute(DataPacket input, ControlsOutput output, Tactic tactic) {
    bot.botRenderer.setIntersectionTarget(tactic.getTargetPosition());

    if (path == null || path.isOffCourse() || path.getEndTime() < input.car.elapsedSeconds) {

      Optional<Path> tempPath = PathPlanner.doShotPlanning(input);
      if (tempPath.isPresent()) {
        path = tempPath.get();
        try {
          path.lockAndSegment();
          path.extendThroughBall();
        } catch (IllegalStateException e) {
//          e.printStackTrace();
          path = null;
          return;
        }
      } else {
        logger.info("Replan path. None Found.");
        return;
      }
    }

    bot.botRenderer.renderPath(input, path);
    pathExecutor.executePath(input, output, path);
  }
}
