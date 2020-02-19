package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.common.Accels;
import com.eru.rlbot.bot.common.AerialLookUp;
import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.common.DemoChecker;
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

  private boolean useHumanExecution;

  private BallPredictionUtil.ExaminedBallData target;

  @Override
  public void execute(DataPacket input, ControlsOutput output, Tactic tactic) {
    checkTarget(input);

    if (target == null) {
      bot.botRenderer.setBranchInfo("No Aerial target found");
      return; // TODO: Fix this.
    }

    bot.botRenderer.renderTarget(computeInterceptLocation(target.ball.position, tactic.object));
    if (input.car.hasWheelContact && input.car.position.z < 50) {
      output
          .withThrottle(1.0f)
          .withSteer(Angles.flatCorrectionAngle(input.car, target.ball.position));

      double flatDistance = target.ball.position.flatten().distance(input.car.position.flatten());

      // TODO: Figure out if we can hit a ball by jumping here.
      double flatTimeToBall = flatDistance / input.car.groundSpeed;
      double airTime = target.ball.position.z * 3.5 / Constants.BOOSTED_ACCELERATION;

      bot.botRenderer.setBranchInfo("Drive toward ball");
      if (airTime > flatTimeToBall && Math.abs(Angles.flatCorrectionAngle(input.car, target.ball.position)) < .1) {
        tacticManager.preemptTactic(tactic.withType(Tactic.TacticType.FAST_AERIAL));
        useHumanExecution = !useHumanExecution;
      }
    } else {
      bot.botRenderer.setBranchInfo("Plan / Execute aerial");
      Pair<FlightPlan, FlightLog> pair = makePlan(input, target, tactic.object);

      humanExecution(input, output, pair.getFirst());
    }
  }

  private static double ROTATION_TIME = .25;
  private static double FAST_AERIAL_TIME = .25;
  private static double FAST_AERIAL_BOOST = FAST_AERIAL_TIME * Constants.BOOST_RATE;

  private void checkTarget(DataPacket input) {
    if (!DemoChecker.wasDemoed(input.car) && target != null && target.ball.elapsedSeconds < input.car.elapsedSeconds) {
      return;
    }

    BallPredictionUtil ballPredictionUtil = BallPredictionUtil.forIndex(input.car.playerIndex);

    bot.botRenderer.addAlertText("Pick aerial target");

    List<BallPredictionUtil.ExaminedBallData> predictions = Lists.everyNth(ballPredictionUtil.getPredictions(), 5);
    for (BallPredictionUtil.ExaminedBallData prediction : predictions) {
      BallData ball = prediction.ball;

      AerialLookUp.AerialInfo aerialInfo = AerialLookUp.averageBoost(ball.position.z);

      // Path is turn + straight
      Path path = PathPlanner.fastPath(input.car, ball);

      double timeToJump = ball.elapsedSeconds - input.car.elapsedSeconds - aerialInfo.timeToApex - ROTATION_TIME - FAST_AERIAL_TIME;
      double averageSpeed =
          Accels.averageSpeed(input.car.velocity.magnitude(), timeToJump, input.car.boost - aerialInfo.boostAmount - FAST_AERIAL_BOOST);

      // Fast plan + .25 for fast aerial + .25 for rotation + aerialInfo time.
      double groundDistance = path.length() - aerialInfo.horizontalTravel - (averageSpeed * aerialInfo.timeToApex);
      double actualGroundTravel = timeToJump * averageSpeed;

      boolean hittable = groundDistance < actualGroundTravel;
      prediction.setHittable(hittable);
    }

    target = ballPredictionUtil.getFirstHittableLocation();
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

    Vector3 w = input.car.angularVelocity.multiply(-100);
    bot.botRenderer.renderProjection(Color.CYAN, input.car, input.car.position.plus(w));
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

  // https://samuelpmish.github.io/notes/RocketLeague/aerial_hit/
  private void chipExecution(DataPacket input, ControlsOutput output, FlightPlan plan, FlightLog flightLog) {
    flightLog.manageCycle(input.car.elapsedSeconds);

    double sensitivity = flightLog.getBoostAccel() / Constants.BOOSTED_ACCELERATION;
    double offset = input.car.orientation.getNoseVector().angle(flightLog.averageBoostVector);

    // If we are pointed roughly in the correct direction, boost.
    if (offset < sensitivity) {
      boolean pressBoost = flightLog.getBoostAccel() >
          BoostTracker.forCar(input.car).getCommitment() * Constants.BOOSTED_ACCELERATION * Constants.STEP_SIZE;
      output.withBoost(pressBoost);

      // Accurately track boost cycles.
      if (pressBoost || BoostTracker.forCar(input.car).isBoosting()) {
        flightLog.trackBoostFrame();
      }
    }

    pointAt(input.car, flightLog.averageBoostVector, output);

    renderAveragePath(input, plan, flightLog);
  }

  private void renderHumanPath(DataPacket input, FlightPlan plan, FlightTrajectory trajectory) {
    bot.botRenderer.renderTarget(Color.GREEN, trajectory.flightTerminus);
    bot.botRenderer.renderPath(Color.GREEN, Lists.everyNth(trajectory.flightPath, 10));
    bot.botRenderer.renderProjection(
        Color.GREEN,
        input.car,
        input.car.position.plus(plan.computeDeviation(trajectory)));
//    bot.botRenderer.addDebugText(Color.GREEN, "Target / current trajectory diff");

//    Vector3 offSet = plan.interceptLocation.minus(trajectory.flightTerminus);
//    bot.botRenderer.renderProjection(
//        Color.white,
//        input.car,
//        input.car.position.plus(offSet));

    bot.botRenderer.renderProjection(
        Color.ORANGE,
        input.car,
        input.car.position.plus(input.car.orientation.getNoseVector().multiply(300)));
  }

  private void renderAveragePath(DataPacket input, FlightPlan plan, FlightLog flightLog) {
    bot.botRenderer.renderTarget(Color.RED, plan.interceptLocation);
    // Renders the boost vector
    bot.botRenderer.renderProjection(
        Color.RED,
        input.car,
        input.car.position.plus(flightLog.averageBoostVector));
//    bot.botRenderer.addDebugText(Color.RED, "Feather boost\n  to target");
  }

  private void pointAt(CarData car, Vector3 desiredVector, ControlsOutput output) {
    Vector3 nose = desiredVector.normalize();
    Vector3 sideDoor = nose.cross(Vector3.of(0, 0, -1)).normalize();
    Vector3 roofOrientation = nose.cross(sideDoor);
    Orientation carOrientation = Orientation.noseRoof(nose, roofOrientation);

    Angles3.setControlsFor(car, carOrientation.getOrientationMatrix(), output);
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
