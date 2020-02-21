package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.common.Accels;
import com.eru.rlbot.bot.common.AerialLookUp;
import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.common.Pair;
import com.eru.rlbot.bot.common.Path;
import com.eru.rlbot.bot.common.PathPlanner;
import com.eru.rlbot.bot.main.Agc;
import com.eru.rlbot.bot.strats.BallPredictionUtil;
import com.eru.rlbot.common.Lists;
import com.eru.rlbot.common.boost.BoostTracker;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;
import java.awt.Color;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Manages doing aerials. */
public class AerialTactician extends Tactician {

  private static final Logger logger = LogManager.getLogger("AerialTactician");

  AerialTactician(Agc bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  private BallPredictionUtil.ExaminedBallData target;

  @Override
  public void internalExecute(DataPacket input, ControlsOutput output, Tactic tactic) {
    target = getTarget(input);

    if (target == null) {
      bot.botRenderer.setBranchInfo("No Aerial target found");
      return; // TODO: Fix this.
    }

    float timeToImpact = target.ball.elapsedSeconds - input.car.elapsedSeconds;

    bot.botRenderer.renderTarget(computeInterceptLocation(target.ball.position, tactic.object));
    if (input.car.hasWheelContact && input.car.position.z < 50) {

      // TODO: Put this into a sort of aerial planning object.
      Path fastPath = PathPlanner.fastPath(input.car, target.ball);

      AerialLookUp.AerialInfo aerialProfile = AerialLookUp.averageBoost(target.ball.position.z);
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

      double correctionAngle = Angles.flatCorrectionAngle(input.car, target.ball.position);
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

  private static double ROTATION_TIME = .5;
  private static double FAST_AERIAL_TIME = .25;
  private static double FAST_AERIAL_BOOST = FAST_AERIAL_TIME * Constants.BOOST_RATE;
  private static double AERIAL_EFFICIENCY = .25;

  private BallPredictionUtil.ExaminedBallData getTarget(DataPacket input) {
    BallPredictionUtil ballPredictionUtil = BallPredictionUtil.forIndex(input.car.playerIndex);

    BallPredictionUtil.ExaminedBallData firstHittable = ballPredictionUtil.getFirstHittableLocation();
    if (firstHittable != null) {
      return firstHittable;
    }

    List<BallPredictionUtil.ExaminedBallData> predictions = Lists.everyNth(ballPredictionUtil.getPredictions(), 5);
    for (BallPredictionUtil.ExaminedBallData prediction : predictions) {
      BallData ball = prediction.ball;

      AerialLookUp.AerialInfo aerialInfo = AerialLookUp.averageBoost(ball.position.z);

      // Path is turn + straight
      Path path = PathPlanner.fastPath(input.car, ball);

      double jumpTime = aerialInfo.timeToApex + ROTATION_TIME + FAST_AERIAL_TIME;
      double timeToImpact = ball.elapsedSeconds - input.car.elapsedSeconds;
      double timeToJump = timeToImpact - jumpTime;
      double boostReserve = input.car.boost - aerialInfo.boostAmount - FAST_AERIAL_BOOST;
      Accels.AccelResult acceleration = Accels.accelerateForTime(input.car.groundSpeed, timeToJump, boostReserve);

      // Fast plan + .25 for fast aerial + .25 for rotation + aerialInfo time.
      double carriedDistance = acceleration.speed * jumpTime;
      double groundDistance = path.length() - aerialInfo.horizontalTravel - carriedDistance;

      boolean hittable = timeToJump > 0
          && groundDistance < acceleration.distance
          && input.car.boost > aerialInfo.boostAmount;
      prediction.setHittable(hittable);
    }

    return ballPredictionUtil.getFirstHittableLocation();
  }

  private boolean freestyle = false;

  private void humanExecution(DataPacket input, ControlsOutput output, FlightPlan plan) {
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
    ImmutableList.Builder<Vector3> flightPathBuilder = ImmutableList.builder();
    double time = input.car.elapsedSeconds;

    Vector3 velocity = input.car.velocity;
    Vector3 flightTerminus = input.car.position;
    while (time < plan.interceptTime) {
      flightTerminus = flightTerminus.plus(velocity.multiply(Constants.STEP_SIZE));
      velocity = velocity.addZ(Constants.NEG_GRAVITY * Constants.STEP_SIZE);
      time += Constants.STEP_SIZE;

      flightPathBuilder.add(flightTerminus);
    }

    return new FlightTrajectory(flightTerminus, flightPathBuilder.build());
  }

  private void renderHumanPath(DataPacket input, FlightPlan plan, FlightTrajectory trajectory) {
    bot.botRenderer.renderTarget(Color.GREEN, trajectory.flightTerminus);
    bot.botRenderer.renderPath(Color.GREEN, Lists.everyNth(trajectory.flightPath, 10));
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

  private void pointAnyDirection(CarData car, Vector3 desiredVector, ControlsOutput output) {
    Vector3 nose = desiredVector.normalize();
    Vector3 sideDoor = nose.cross(car.orientation.getRoofVector().multiply(-1));
    Vector3 roofOrientation = nose.cross(sideDoor);
    Orientation carOrientation = Orientation.noseRoof(nose, roofOrientation);

    Angles3.setControlsFor(car, carOrientation.getOrientationMatrix(), output);
  }

  private static Pair<FlightPlan, FlightLog> makePlan(DataPacket input, BallPredictionUtil.ExaminedBallData target, Vector3 targetResult) {
    Vector3 interceptLocation = computeInterceptLocation(target.ball.position, targetResult);

    float flightDuration =
        (float) ((input.car.position.flatten().distance(interceptLocation.flatten()) / input.car.velocity.magnitude()) * .95);
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

  private static Vector3 computeInterceptLocation(Vector3 ballPosition, Vector3 targetPosition) {
    return ballPosition.minus(targetPosition)
        .toMagnitude(Constants.BALL_RADIUS)
        .plus(ballPosition);
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
    private final List<Vector3> flightPath;

    public FlightTrajectory(Vector3 flightTerminus, List<Vector3> flightPath) {
      this.flightTerminus = flightTerminus;
      this.flightPath = flightPath;
    }
  }
}
