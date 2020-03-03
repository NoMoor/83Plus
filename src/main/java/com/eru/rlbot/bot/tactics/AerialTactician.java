package com.eru.rlbot.bot.tactics;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.eru.rlbot.bot.common.Accels;
import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.lookup.AerialLookUp;
import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
import com.eru.rlbot.bot.path.Path;
import com.eru.rlbot.bot.path.PathPlanner;
import com.eru.rlbot.bot.prediction.BallPredictionUtil;
import com.eru.rlbot.bot.prediction.CarPrediction;
import com.eru.rlbot.common.Lists;
import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.Pair;
import com.eru.rlbot.common.boost.BoostTracker;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.output.Controls;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.awt.Color;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Manages doing aerials. */
public class AerialTactician extends Tactician {

  private static final Logger logger = LogManager.getLogger("AerialTactician");

  AerialTactician(ApolloGuidanceComputer bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  private Moment target;

  @Override
  public void internalExecute(DataPacket input, Controls output, Tactic tactic) {
    target = tactic.subject;

    float timeToImpact = target.time - input.car.elapsedSeconds;
    bot.botRenderer.renderText(Color.RED, target.position, "" + timeToImpact);

    bot.botRenderer.renderTarget(Color.CYAN, target.position);
    bot.botRenderer.renderTarget(Color.WHITE, computeInterceptLocation(target, tactic.object));
    if (input.car.hasWheelContact && input.car.position.z < 50) {

      // TODO: Put this into a sort of aerial planning object.
      Path fastPath = PathPlanner.fastPath(input.car, target);

      AerialLookUp.AerialInfo aerialProfile = AerialLookUp.averageBoost(target.position.z);
      double airTime = FAST_AERIAL_TIME + aerialProfile.timeToApex;
      double groundTime = timeToImpact - airTime;

      // The distance traveled if we jump at our current speed.
      double carriedGroundDistance = airTime * input.car.groundSpeed;

      // TODO: Move the Fast aerial boost into the aerial profile.
      // The boost beyond what we need.
      double boostReserves = input.car.boost - aerialProfile.boostAmount - FAST_AERIAL_BOOST;

      double aerialMargin = aerialProfile.nonBoostingTime * (boostReserves * .15 / Constants.BOOST_RATE) * Constants.BOOSTED_ACCELERATION * AERIAL_EFFICIENCY;

      // The distance to travel on the ground.
      double groundDistance = fastPath.length() - carriedGroundDistance - aerialProfile.horizontalTravel - aerialMargin;

      // The average speed needed to travel on the ground.
      double requiredGroundSpeed = groundDistance / groundTime;

      if (requiredGroundSpeed > input.car.groundSpeed) {
        // Need to accelerate.
        if (boostReserves > Constants.MIN_BOOST_BURST) {
          output.withBoost();
        }
        output.withThrottle(1.0);
      } else {
        // TODO: Figure out how much to slow down.
      }

      double correctionAngle = Angles.flatCorrectionAngle(input.car, target.position);
      output.withSteer(correctionAngle * 5);

      bot.botRenderer.setBranchInfo("Drive toward ball");
      if (boostReserves > 0 && (groundTime < Constants.STEP_SIZE) && Math.abs(correctionAngle) < .05) {
        tacticManager.preemptTactic(tactic.withType(Tactic.TacticType.FAST_AERIAL));
      }
    } else {
      bot.botRenderer.setBranchInfo("Plan / Execute aerial");
      Pair<FlightPlan, FlightLog> pair = makePlan(input, target, tactic.object);

      humanExecution(input, output, pair.getFirst());
    }
  }

  private static double FAST_AERIAL_TIME = .25;
  private static double FAST_AERIAL_BOOST = FAST_AERIAL_TIME * Constants.BOOST_RATE;
  private static double AERIAL_EFFICIENCY = .25;

  // TODO: Move this into a common planning location.
  public static void doAerialPlanning(DataPacket input) {
    // Only do planning when you are on the ground.
    if (!input.car.hasWheelContact && input.car.position.z < 20) {
      return;
    }

    BallPredictionUtil ballPredictionUtil = BallPredictionUtil.get(input.car.serialNumber);

    List<BallPredictionUtil.ExaminedBallData> predictions = Lists.everyNth(ballPredictionUtil.getPredictions(), 5);
    for (BallPredictionUtil.ExaminedBallData prediction : predictions) {
      BallData ball = prediction.ball;

      if (ball.position.z < 300) {
        continue;
      }

      AerialLookUp.AerialInfo aerialInfo = AerialLookUp.averageBoost(ball.position.z);

      // Path is turn + straight
      Path path = PathPlanner.fastPath(input.car, ball);

      double rotationTime = aerialInfo.boostAngle * 2 * .3; // Boost angle and back to flat with 2 radians per second
      double jumpTime = aerialInfo.timeToApex + rotationTime + FAST_AERIAL_TIME;
      double timeToImpact = ball.time - input.car.elapsedSeconds;

      // TODO: Handle the case where we are already in the air.
      double timeToJump = timeToImpact - jumpTime;
      double boostReserve = input.car.boost - aerialInfo.boostAmount - FAST_AERIAL_BOOST;
      Accels.AccelResult acceleration = Accels.accelerateForTime(input.car.groundSpeed, timeToJump, boostReserve);

      // Fast plan + .25 for fast aerial + .25 for rotation + aerialInfo time.
      double carriedDistance = acceleration.speed * jumpTime;
      double groundDistance = path.length() - aerialInfo.horizontalTravel - carriedDistance;

      if (timeToJump > 0
          && groundDistance < acceleration.distance
          && input.car.boost > aerialInfo.boostAmount) {
        prediction.setHittableBy(Tactic.TacticType.AERIAL);
      }
    }
  }

  private boolean freestyle = false;

  private void humanExecution(DataPacket input, Controls output, FlightPlan plan) {
    FlightTrajectory trajectory = computeFlightTrajectory(input, plan);
    Vector3 flightPlanDiff = plan.computeDeviation(trajectory);
    float timeToImpact = plan.interceptTime - input.car.elapsedSeconds;

    Vector3 carTarget = input.ball.position.minus(input.car.position);
    double targetNoseAngle = carTarget.angle(input.car.orientation.getNoseVector());
    if (flightPlanDiff.magnitude() > 20 && flightPlanDiff.angle(input.car.orientation.getNoseVector()) > .05) {
      pointAnyDirection(input.car, flightPlanDiff, output);
    } else if (timeToImpact * 2 < targetNoseAngle) {
      pointAnyDirection(input.car, carTarget, output);
    } else if (freestyle) {
      output.withRoll(1);
    }

    double rotationalOffset = input.car.angularVelocity.angle(flightPlanDiff);

    if (flightPlanDiff.magnitude() > 20) {
      double sensitivity = .1f;
      double noseOffset = input.car.orientation.getNoseVector().angle(flightPlanDiff);
      if (noseOffset < sensitivity || rotationalOffset < sensitivity * 4) {
        double offTargetDistance = flightPlanDiff.magnitude();
        double boostDistanceChange = timeToImpact * Constants.BOOSTED_ACCELERATION * Constants.STEP_SIZE * getBoostCommitment(input);
        boolean boosting = offTargetDistance > boostDistanceChange;
        output.withBoost(boosting);
      } else {
        bot.botRenderer.setBranchInfo("Nose off angle");
      }
    } else {
      bot.botRenderer.setBranchInfo("Boost not needed");
    }

    renderHumanPath(input, plan, trajectory);
  }

  private float getBoostCommitment(DataPacket input) {
    return BoostTracker.forCar(input.car).getCommitment();
  }

  private FlightTrajectory computeFlightTrajectory(DataPacket input, FlightPlan plan) {
    return new FlightTrajectory(CarPrediction.noInputs(input.car, plan.interceptTime - input.car.elapsedSeconds));
  }

  private void renderHumanPath(DataPacket input, FlightPlan plan, FlightTrajectory trajectory) {
    bot.botRenderer.renderTarget(Color.GREEN, trajectory.flightTerminus);
    bot.botRenderer.renderPath(
        Color.GREEN,
        Lists.everyNth(trajectory.flightPath, 10).stream()
            .map(CarPrediction.PredictionNode::getPosition)
            .collect(toImmutableList()));
    bot.botRenderer.renderProjection(
        Color.GREEN,
        input.car,
        input.car.position.plus(plan.computeDeviation(trajectory)));
//    bot.botRenderer.addDebugText(Color.GREEN, "Target / current trajectory diff");

    bot.botRenderer.renderProjection(
        Color.ORANGE,
        input.car,
        input.car.position.plus(input.car.orientation.getNoseVector().multiply(300)));
  }

  private void pointAnyDirection(CarData car, Vector3 desiredVector, Controls output) {
    Vector3 nose = desiredVector.normalize();
    Vector3 sideDoor = nose.cross(car.orientation.getRoofVector().multiply(-1));
    Vector3 roofOrientation = nose.cross(sideDoor);
    Orientation carOrientation = Orientation.noseRoof(nose, roofOrientation);

    Angles3.setControlsFor(car, carOrientation.getOrientationMatrix(), output);
  }

  private static Pair<FlightPlan, FlightLog> makePlan(DataPacket input, Moment target, Vector3 targetResult) {
    Vector3 interceptLocation = computeInterceptLocation(target, targetResult);

//    float flightDuration =
//        (float) ((input.car.position.flatten().distance(interceptLocation.flatten()) / input.car.velocity.magnitude()) * .95);
    float flightDuration = target.time - input.car.elapsedSeconds;
    float startBoostTime = flightDuration * .25f;
    Vector3 boostVector = calculateBoost(input.car, interceptLocation, flightDuration, startBoostTime);

    return Pair.of(
        new FlightPlan(interceptLocation, input.car.elapsedSeconds, flightDuration),
        new FlightLog(input.car.elapsedSeconds + flightDuration * .25f, boostVector));
  }

  private static Vector3 calculateBoost(CarData car, Vector3 interceptLocation, double flightDuration, double boostTime) {
    double startBoostTime = flightDuration - boostTime;

    double coefficient = 2 / (flightDuration * (flightDuration - (2 * startBoostTime)));
    return interceptLocation
        .minus(car.position)
        .minus(car.velocity.multiply(flightDuration))
        .minus(Vector3.of(0, 0, Constants.NEG_GRAVITY).multiply(.5 * (flightDuration * flightDuration)))
        .multiply(coefficient);
  }

  private static Vector3 computeInterceptLocation(Moment moment, Vector3 targetPosition) {
    if (moment.type == Moment.Type.BALL) {
      return moment.position.minus(targetPosition)
          .toMagnitude(Constants.BALL_RADIUS)
          .plus(moment.position);
    } else {
      return moment.position;
    }
  }

  private static class FlightPlan {
    final float interceptTime;
    final float flightDuration;
    final Vector3 interceptLocation;

    public FlightPlan(Vector3 interceptLocation, float currentTime, float flightDuration) {
      this.interceptLocation = interceptLocation;
      this.interceptTime = currentTime + flightDuration;
      this.flightDuration = flightDuration;
    }

    public Vector3 computeDeviation(FlightTrajectory trajectory) {
      return interceptLocation.minus(trajectory.flightTerminus);
    }
  }

  private static class FlightLog {
    final double startBoostTime;
    final Vector3 averageBoostVector;
    private double boostAccel;

    private FlightLog(double startBoostTime, Vector3 averageBoostVector) {
      this.startBoostTime = startBoostTime;
      this.averageBoostVector = averageBoostVector;
    }

    public void manageCycle(double currentTime) {
      if (currentTime > startBoostTime)
        boostAccel += averageBoostVector.magnitude() * Constants.STEP_SIZE;
    }

    public double getBoostAccel() {
      return boostAccel;
    }

    public void trackBoostFrame() {
      boostAccel -= Constants.BOOSTED_ACCELERATION * Constants.STEP_SIZE;
    }

    public boolean isHittable() {
      return averageBoostVector.magnitude() < Constants.BOOSTED_ACCELERATION * .9;
    }
  }

  private static class FlightTrajectory {

    private final Vector3 flightTerminus;
    private final ImmutableList<CarPrediction.PredictionNode> flightPath;

    public FlightTrajectory(ImmutableList<CarPrediction.PredictionNode> flightPath) {
      this.flightTerminus = Iterables.getLast(flightPath).position;
      this.flightPath = flightPath;
    }
  }
}
