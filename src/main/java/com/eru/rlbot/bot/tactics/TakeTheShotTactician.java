package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.common.*;
import com.eru.rlbot.bot.main.Agc;
import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.flat.BallPrediction;
import rlbot.flat.PredictionSlice;
import java.util.Optional;

// Relies on other low level tactical units to do the movement but this tactician is responsible for planning a shot on
// goal.
public class TakeTheShotTactician extends Tactician {

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

  public static Moment shotTarget(DataPacket input) {
    // Assume we are hitting the ball where it is.
    Optional<BallPrediction> ballPredictionOptional = DllHelper.getBallPrediction();

    if (ballPredictionOptional.isPresent()) {

      // TODO: Take into account angle change.
      BallPrediction ballPrediction = ballPredictionOptional.get();
      Vector2 flatCarPosition = input.car.position.flatten();

      for (int i = 0 ; i < ballPrediction.slicesLength() ; i++) {
        PredictionSlice slice = ballPrediction.slices(i);

        Vector3 slicePosition = Vector3.of(slice.physics().location());

        double timeToLocation = Accels.minTimeToDistance(
            input.car,
            flatCarPosition.distance(slicePosition.flatten()) - Constants.BALL_RADIUS - Constants.CAR_LENGTH)
            .time;

        // TODO: Account for more time to swing out.
        if (timeToLocation < slice.gameSeconds() - input.car.elapsedSeconds) {
          // Target Acquired.
          return new Moment(slice);
        }
      }
    }

    return new Moment(input.ball.position, input.ball.velocity);
  }

  private static double timeToBall(BallData relativeBall, CarData car) {
    return car.boost > 40
        ? Accels.minTimeToDistance(car, relativeBall.position.flatten().norm()).time
        : Accels.timeToDistance(car.velocity.flatten().norm(), relativeBall.position.flatten().norm()).time;
  }

  private Path path;

  @Override
  public boolean isLocked() {
    return true;
  }

  @Override
  public void execute(DataPacket input, ControlsOutput output, Tactic tactic) {
    bot.botRenderer.setIntersectionTarget(tactic.getTargetPosition());

    if (path == null || path.isOffCourse() || path.getEndTime() < input.car.elapsedSeconds) {
//      bot.botRenderer.addAlertText("New Path %f", input.car.elapsedSeconds);
      path = PathPlanner.doShotPlanning(input);
      path.lockAndSegment(input);
      path.extendThroughBall();
    }

    bot.botRenderer.renderPath(input, path);
    pathExecutor.executePath(input, output, path);
  }
}
