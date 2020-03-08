package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.common.Accels;
import com.eru.rlbot.bot.common.RelativeUtils;
import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
import com.eru.rlbot.bot.path.Path;
import com.eru.rlbot.bot.path.PathPlanner;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.Controls;
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

  public static boolean takeTheShot(DataPacket input) {
    if (input.allCars.size() == 1) {
      return true;
    }

    BallData relativeBallData = RelativeUtils.noseRelativeBall(input);

    // TODO: Replace this with another time to ball equation.
    double timeToBall = timeToBall(relativeBallData, input.car);

    // Am the closest car to the ball.
    for (int i = 0; i < input.allCars.size(); i++) {
      CarData nextCar = input.allCars.get(i);
      if (nextCar == input.car) {
        continue;
      }

      BallData relativeBallDataI = RelativeUtils.noseRelativeBall(input, i);

      double oppTimeToBall = timeToBall(relativeBallDataI, nextCar);

      if (oppTimeToBall < timeToBall + .25) {
        return false;
      }
    }

    return true;
  }

  private static double timeToBall(BallData relativeBall, CarData car) {
    return car.boost > 40
        ? Accels.minTimeToDistance(car, relativeBall.position.flatten().magnitude()).time
        : Accels.nonBoostedTimeToDistance(car.velocity.flatten().magnitude(), relativeBall.position.flatten().magnitude()).time;
  }

  @Override
  public boolean isLocked() {
    return false;
  }

  @Override
  public void internalExecute(DataPacket input, Controls output, Tactic tactic) {
    bot.botRenderer.setIntersectionTarget(tactic.getTargetPosition());

    Path path = PathPlanner.oneTurn(input.car, tactic.subject);
    pathExecutor.executePath(input, output, path);

    bot.botRenderer.renderPath(input, path);
    pathExecutor.executePath(input, output, path);
  }
}
