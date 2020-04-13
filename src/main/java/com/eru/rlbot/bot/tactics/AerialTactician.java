package com.eru.rlbot.bot.tactics;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.eru.rlbot.bot.common.Accels;
import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.lookup.AerialLookUp;
import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
import com.eru.rlbot.bot.maneuver.WallHelper;
import com.eru.rlbot.bot.path.Path;
import com.eru.rlbot.bot.path.PathPlanner;
import com.eru.rlbot.bot.path.Plan;
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
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manages doing aerials.
 */
public class AerialTactician extends Tactician {

  private static final Logger logger = LogManager.getLogger("AerialTactician");

  private boolean locked;

  AerialTactician(ApolloGuidanceComputer bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  @Override
  public boolean isLocked() {
    return locked;
  }

  @Override
  public void internalExecute(DataPacket input, Controls output, Tactic tactic) {
    if (WallHelper.isOnWall(input.car)) {
      WallHelper.drive(input, output, tactic.subject.position);
      return;
    }

    Moment target = tactic.subject;

    float timeToImpact = target.time - input.car.elapsedSeconds;

    bot.botRenderer.renderTarget(Color.CYAN, tactic.object);
    bot.botRenderer.renderTarget(Color.WHITE, tactic.subject.position);
    bot.botRenderer.renderTarget(Color.MAGENTA, computeInterceptLocation(target, tactic.object));
    if (input.car.hasWheelContact && input.car.position.z < 50) {
      locked = false;

      // TODO: Put this into a sort of aerial planning object.
      Path oneTurn = PathPlanner.oneTurn(input.car, target);

      if (oneTurn == null) {
        logger.info("Got null path. Doing nothing.");
        return;
      }

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
      double groundDistance = oneTurn.length() - carriedGroundDistance - aerialProfile.horizontalTravel - aerialMargin;

      // The average speed needed to travel on the ground.
      double requiredGroundSpeed = groundDistance / groundTime;

      if (requiredGroundSpeed > input.car.groundSpeed) {
        // Need to accelerate.
        if (boostReserves > Constants.MIN_BOOST_BURST) {
          output.withBoost();
        }
        output.withThrottle(1.0);
      } else {
        output.withThrottle(-1.0);
        // TODO: Figure out how much to slow down.
      }

      double correctionAngle = Angles.flatCorrectionAngle(input.car, target.position);
      output.withSteer(correctionAngle * 5);


      // TODO: Break more?
      bot.botRenderer.setBranchInfo("Drive toward ball");
      if (boostReserves > 0 && (groundTime < Constants.STEP_SIZE) && Math.abs(correctionAngle) < .05) {
        tacticManager.preemptTactic(tactic.withType(Tactic.TacticType.FAST_AERIAL));
        locked = true;
      }
    } else {
      bot.botRenderer.setBranchInfo("Plan / Execute aerial");
      Pair<FlightPlan, FlightLog> pair = makePlan(input, target, tactic.object);

      humanExecution(input, output, pair.getFirst(), pair.getSecond());
    }

    Vector3 carBall = input.ball.position.minus(input.car.position);
    if (input.car.orientation.getNoseVector().dot(carBall) > 0) {
      locked = false;
    }
  }

  private static double FAST_AERIAL_TIME = .25;
  private static double FAST_AERIAL_BOOST = FAST_AERIAL_TIME * Constants.BOOST_RATE;
  private static double AERIAL_EFFICIENCY = .25;

  public static Optional<Plan> doAerialPlanning(CarData car, BallData ball) {
    AerialLookUp.AerialInfo aerialInfo = AerialLookUp.averageBoost(ball.position.z);

    // Path is turn + straight
    Path oneTurn = PathPlanner.oneTurn(car, ball);
    if (oneTurn == null) {
      return Optional.empty();
    }

    double rotationTime = aerialInfo.boostAngle * 2 * .3; // Boost angle and back to flat with 2 radians per second
    double jumpTime = aerialInfo.timeToApex + rotationTime + FAST_AERIAL_TIME;
    double timeToImpact = ball.time - car.elapsedSeconds;

    // TODO: Handle the case where we are already in the air.
    double timeToJump = timeToImpact - jumpTime;
    double boostReserve = car.boost - aerialInfo.boostAmount - FAST_AERIAL_BOOST;
    Accels.AccelResult acceleration = Accels.accelerateForTime(car.groundSpeed, timeToJump, boostReserve);

    // Fast plan + .25 for fast aerial + .25 for rotation + aerialInfo time.
    double carriedDistance = acceleration.speed * jumpTime;
    double groundDistance = oneTurn.length() - aerialInfo.horizontalTravel - carriedDistance;

    if (timeToJump > 0
        && groundDistance < acceleration.distance
        && car.boost > aerialInfo.boostAmount) {

      return Optional.of(
          Plan.builder()
              .setTacticType(Tactic.TacticType.AERIAL)
              .setPath(oneTurn)
              .setBoostUsed(aerialInfo.boostAmount + acceleration.boost)
              .build(ball.time - car.elapsedSeconds));
    } else {
      return Optional.empty();
    }
  }

  private boolean freestyle = false;

  private void humanExecution(DataPacket input, Controls output, FlightPlan plan, FlightLog second) {

    bot.botRenderer.renderTarget(Color.GREEN, plan.setupLocation);

    // TODO: If we have enough boost, Match the angle of impact first and then boost to hit the ball.
    FlightTrajectory trajectory = computeFlightTrajectory(input, plan);
    boolean isSetupPhase = plan.isSetupPhase(trajectory);
    Vector3 setupDeviation = plan.setUpDeviation(trajectory);
    Vector3 terminalDeviation = plan.terminalDeviation(trajectory);
    float timeToImpact = plan.interceptTime - input.car.elapsedSeconds;

    Vector3 carTarget = input.ball.position.minus(input.car.position);
    double targetNoseAngle = carTarget.angle(input.car.orientation.getNoseVector());
    if (isSetupPhase) {
      pointAnyDirection(input.car, setupDeviation, output);
    } else if (terminalDeviation.magnitude() > 20 && terminalDeviation.angle(input.car.orientation.getNoseVector()) > .05) {
      pointAnyDirection(input.car, terminalDeviation, output);
    } else if (timeToImpact * 2 < targetNoseAngle) {
      pointAnyDirection(input.car, carTarget, output);
    } else if (freestyle) {
      output.withRoll(1);
    }

    Vector3 trackedAngle = isSetupPhase ? setupDeviation : terminalDeviation;

    double rotationalOffset = input.car.angularVelocity.angle(trackedAngle);

    if (trackedAngle.magnitude() > 20) {
      double sensitivity = .1f;
      double noseOffset = input.car.orientation.getNoseVector().angle(trackedAngle);
      if (noseOffset < sensitivity || rotationalOffset < sensitivity * 4) {
        double offTargetDistance = trackedAngle.magnitude();
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
        input.car.position.plus(plan.terminalDeviation(trajectory)));

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

    float flightDuration = target.time - input.car.elapsedSeconds;
    float startBoostTime = flightDuration * .25f;
    Vector3 boostVector = calculateBoost(input.car, interceptLocation, flightDuration, startBoostTime);

    return Pair.of(
        new FlightPlan(interceptLocation, input.car.elapsedSeconds, flightDuration, target),
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
      // Assume that we hit the ball hard (~2000) and aim for the top.
      Vector3 ballToTarget = moment.position.minus(targetPosition);
      double flatDistance = ballToTarget.flatten().magnitude();
      double travelTime = flatDistance / 2000;
      double verticalDrop = Accels.verticalDistance(moment.velocity.z, travelTime);

      // Pretend the target is higher than it is.
      Vector3 adjustedTarget = targetPosition.addZ(-verticalDrop);
      Vector3 newTargetToBall = moment.position.minus(adjustedTarget);

      return newTargetToBall
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
    final Moment target;
    final Vector3 setupLocation;

    public FlightPlan(Vector3 interceptLocation, float currentTime, float flightDuration, Moment target) {
      this.interceptLocation = interceptLocation;
      this.target = target;
      this.interceptTime = currentTime + flightDuration;
      this.flightDuration = flightDuration;
      Vector3 setupOffset = interceptLocation.minus(target.position).toMagnitude(250);
      setupLocation = interceptLocation.plus(setupOffset);
    }

    public Vector3 setUpDeviation(FlightTrajectory trajectory) {
      return setupLocation.minus(trajectory.flightTerminus);
    }

    public Vector3 terminalDeviation(FlightTrajectory trajectory) {
      return interceptLocation.minus(trajectory.flightTerminus);
    }

    public boolean isSetupPhase(FlightTrajectory trajectory) {
      double distanceToSetup = setupLocation.distance(trajectory.flightTerminus);
      double distanceToTerminus = setupLocation.distance(trajectory.flightTerminus);
      return distanceToSetup > 20 && distanceToSetup < distanceToTerminus;
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
