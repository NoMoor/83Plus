package com.eru.rlbot.bot.ballchaser.v1.strats;

import com.eru.rlbot.bot.ballchaser.v1.tactics.Tactic;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.cppinterop.RLBotDll;
import rlbot.flat.BallPrediction;
import rlbot.flat.PredictionSlice;

import java.util.ArrayList;
import java.util.List;

import static com.eru.rlbot.bot.common.Constants.ACCELERATION;
import static com.eru.rlbot.bot.common.Constants.MAX_SPEED;

public class PathFinder {

  public List<Tactic> findPath(DataPacket input) {
    // TODO: This should probably just adjust the spot that the ball is so any boosts or w/e can stay.
    try {
      double maxScore = Double.MAX_VALUE;

      BallPrediction ballPrediction = RLBotDll.getBallPrediction();

      if (ballPrediction.slicesLength() == 0) {
        return new ArrayList<>();
      }

      // TODO(ahatfield): This is kinda cludged together.
      PredictionSlice bestTarget = ballPrediction.slices(ballPrediction.slicesLength() - 1);
      for (int i = 0 ; i < ballPrediction.slicesLength() ; i++) {
        PredictionSlice predictionSlice = ballPrediction.slices(i);
        double score = createBallScore(input, predictionSlice);
        if (score > 0 && score < maxScore) {
          maxScore = score;
          bestTarget = predictionSlice;
        }
      }

      // Hit the ball to the center of the goal.
      return createPath(input.car, new Tactic(bestTarget, Tactic.Type.HIT_BALL), Goal.opponentGoal(1).center);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return new ArrayList<>();
  }



  // TODO: Move this to another place.
  private List<Tactic> createPath(CarData carData, Tactic tactic, Vector3 targetLocation) {
    // Connect the goal and the tactic in a line.
    Vector3 ballToGoalVector = targetLocation.minus(tactic.target.position);
    // Get a 1 unit directional vector.
    Vector2 glideSlope = ballToGoalVector.normalized().flatten().scaled(1000); // TODO: This scaling should depend on the turn in angle.
    Vector3 attackTarget = tactic.target.position.flatten().minus(glideSlope).asVector3(); // TODO: Draw this.

    // Run through the point on the approach.
//    Tactic approachTactic = new Tactic(attackTarget, Tactic.Type.HIT_BALL);

    // TODO: Make an attack arc tactic to hit the attackTarget.
    // Velocity at that point
    // Calculate speed at that point.

    // Vector from current car position to turn in spot.
    Vector2 carToAttackTarget = carData.position.flatten().minus(attackTarget.flatten());
    double angleToTurnIn = carToAttackTarget.correctionAngle(glideSlope);

    double width = Constants.turnWidth(MAX_SPEED, angleToTurnIn);
    double depth = Constants.turnDepth(MAX_SPEED, angleToTurnIn);

//    tacticManager.setTactic(approachTactic);
    List<Tactic> results = new ArrayList<>();
    results.add(tactic);
    return results;
  }


  private static double createBallScore(DataPacket input, PredictionSlice predictionSlice) {
    Vector3 location = Vector3.of(predictionSlice.physics().location());

    double timeToPosition = timeToPosition(input.car, location);

    double ballTimeToPosition = (predictionSlice.gameSeconds() - input.car.elapsedSeconds);

    // If we cannot get there in time to hit it.
    if (timeToPosition > ballTimeToPosition) {
      // Can't get there in time.
      return Double.MAX_VALUE;
    }

    return predictionSlice.gameSeconds();
  }

  private static double timeToPosition(CarData carData, Vector3 target) {
    // 2D distance
    double distanceToPosition = carData.position.flatten().distance(target.flatten());

    // Am I heading the correct direction?
    Vector3 futurePosition = carData.position.plus(carData.velocity.scaled(5.0 / 60));
    boolean correctDirection = futurePosition.distance(target) < distanceToPosition;

    if (correctDirection) {
      // TODO(ahatfield): Change this to be only the velocity in the correct direction.
      double velocity = carData.velocity.flatten().magnitude();
      double distanceToMaxSpeed = distanceToMaxSpeed(velocity);

      if (distanceToMaxSpeed < distanceToPosition) {
        // 0 = 2at^2 + 2vot - 2d
        return timeAccelerating(velocity, distanceToPosition);
      } else {
        // Will hit max speed first.
        double distanceAtMaxSpeed = distanceToPosition - distanceToMaxSpeed;
        double timeAtMaxSpeed = distanceAtMaxSpeed / MAX_SPEED;

        return timeToMaxSpeed(velocity) + timeAtMaxSpeed;
      }
    } else {
      // time to turn around + time to ball
      // TODO(ahatfield): Plus some distance to turn around.
      return (carData.velocity.magnitude() / ACCELERATION)
          + distanceToPosition / (carData.velocity.flatten().magnitude() + (ACCELERATION / 2));
    }
  }

  private static double timeToMaxSpeed(double velocity) {
    return (MAX_SPEED - velocity) / ACCELERATION;
  }

  /** Assumes no boost. */
  private static double distanceToMaxSpeed(double velocity) {
    // TODO: This should be only the velocity in the correct direction.
    double deltaV = MAX_SPEED - velocity;
    return ((2 * velocity * deltaV) + (deltaV * deltaV)) / (2 * ACCELERATION);
  }

  // Assumes no 'tricks' and infinite speed.
  private static double timeAccelerating(double velocity, double distance) {
    double a = ACCELERATION;
    double b = 2 * velocity;
    double c = -2 * distance;

    double result = b * b - 4.0 * a * c;

    if (result > 0.0) {
      // If these are both positive, it means that ... ???
      double r1 = (-b + Math.pow(result, 0.5)) / (2.0 * a);
      double r2 = (-b - Math.pow(result, 0.5)) / (2.0 * a);
      // Take the lesser positive value.
      return r1 > 0 && r2 > 0
          ? Math.min(r1, r2)
          : r1 < 0
          ? r2
          : r2 < 0
          ? r1
          : Double.MAX_VALUE;
    } else if (result == 0.0) {
      double r = -b / (2.0 * a);
      return r > 0 ? r : Double.MAX_VALUE;
    } else {
      return Double.MAX_VALUE;
    }
  }
}
