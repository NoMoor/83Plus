package com.eru.rlbot.bot.tactics;

import static com.eru.rlbot.bot.lookup.AerialLookUp.*;
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
import com.eru.rlbot.common.Numbers;
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
import java.util.Comparator;
import java.util.List;
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
    return super.isLocked() || locked;
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
    bot.botRenderer.renderTarget(Color.MAGENTA, computeInterceptLocation(input.car, target, tactic.object));
    if (input.car.hasWheelContact && input.car.position.z < 50) {
      locked = false;

      // TODO: Put this into a sort of aerial planning object.
      Moment groundMoment = target.toBuilder()
          .setPosition(target.position.setZ(Constants.BALL_RADIUS))
          .build();
      Path oneTurn = PathPlanner.oneTurn(input.car, groundMoment);

      if (oneTurn == null) {
        logger.info("Got null path. Doing nothing.");
        return;
      }

      bot.botRenderer.renderPath(Color.RED, oneTurn);

      if (true) {
        Vector3 globalFastAerialVelocity = input.car.orientation.global(AerialLookUp.FAST_AERIAL_VELOCITY);
        Vector3 newVelocity = input.car.velocity.plus(globalFastAerialVelocity);

        Vector3 averageVelocity = input.car.velocity.plus(newVelocity).divide(2);
        Vector3 newPosition = input.car.position.plus(averageVelocity.multiply(FAST_AERIAL_TIME));

        CarData postFastAerialCar = input.car.toBuilder()
            .setVelocity(newVelocity)
            .setPosition(newPosition)
            .setTime(input.car.elapsedSeconds + FAST_AERIAL_TIME)
            .build();

        Pair<FlightPlan, FlightLog> plan = makePlan(postFastAerialCar, tactic.subject, tactic.object);
        FlightPlan flightPlan = plan.getFirst();

        double boostRequirement = FAST_AERIAL_BOOST + flightPlan.boostRequirement();

        double boostAngle =
            Math.atan2(flightPlan.averageBoostVector.flatten().magnitude(), flightPlan.averageBoostVector.z);
        boolean goodBoostDirection = boostAngle > 1.5;

        bot.botRenderer.setBranchInfo(
            "Acc: %d B: %d Ang: %.2f",
            (int) flightPlan.averageBoostVector.magnitude(),
            (int) boostRequirement, boostAngle);

        if ((goodBoostDirection || flightPlan.flightDuration > .35) // Either we need to pitch up or the flight is very short.
            && flightPlan.isHittable() && boostRequirement < input.car.boost) {
          startFastAerial(tactic);
          return;
        }

        // TODO else drive.
        // If the boost angle is shallow, we probably should get closer.
        // If the boost angle is behind us but the ball is still in-front of us, can we slow down first?
      }

      // TODO: Update this to check to see if jumping right now has a 'firing solution'.
      List<AerialInfo> aerialProfiles = getInfos(input.car.boost, target.position.z);

      // Get the one that boosts straight up full throttle
      Optional<AerialInfo> aerialProfileOptional = aerialProfiles.stream()
          .filter(aerialInfo -> aerialInfo.boostUsed < input.car.boost + 5)
          .sorted(Comparator.comparing(aerialInfo -> aerialInfo.time))
          .skip(2)
          .findFirst();

      if (!aerialProfileOptional.isPresent()) {
        bot.botRenderer.addAlertText("Cannot find profile");
        return;
      }

      AerialInfo aerialProfile = aerialProfileOptional.get();

      double airTime = aerialProfile.time;
      double timeToJump = timeToImpact - airTime;

      // The distance traveled if we jump at our current speed.
      double forwardSpeed = input.car.orientation.localCoordinates(input.car.velocity).x;

      double carriedGroundDistance = airTime * forwardSpeed;

      // The boost beyond what we need.
      double boostReserves = input.car.boost - aerialProfile.boostUsed;

      // The distance to travel on the ground.
      double groundDistance = oneTurn.length() - carriedGroundDistance - aerialProfile.horizontalTravel;

      // The average speed needed to travel on the ground.
      double requiredGroundSpeed = groundDistance / timeToJump;

      if (requiredGroundSpeed > forwardSpeed) {
        // Need to accelerate.
        if (boostReserves > Constants.MIN_BOOST_BURST) {
          output.withBoost();
        }
        output.withThrottle(1.0);
      } else if (forwardSpeed > 0) {
        output
            .withThrottle(-1.0);
      }

      double correctionAngle = Angles.flatCorrectionAngle(input.car, target.position);
      if (forwardSpeed < 0) {
        // Flip steering if we are traveling backward.
        correctionAngle *= -1;
      }
      output.withSteer(correctionAngle * 5);

      if ((timeToJump < Constants.STEP_SIZE * 10) && Math.abs(correctionAngle) < .1) {
        startFastAerial(tactic);
        return;
      }
    } else {
      bot.botRenderer.setBranchInfo("Plan / Execute aerial");
      Pair<FlightPlan, FlightLog> pair = makePlan(input.car, target, tactic.object);

      humanExecution(input, output, pair.getFirst(), pair.getSecond());
    }

    Vector3 carBall = input.ball.position.minus(input.car.position);
    if (input.car.orientation.getNoseVector().dot(carBall) > 0) {
      locked = false;
    }
  }

  private void startFastAerial(Tactic tactic) {
    delegateTo(new FastAerial(tactic.subject.position));
    locked = true;
  }

  public static Optional<Plan> doAerialPlanning(CarData car, BallData ball) {
    if (!car.hasWheelContact) {
      return inAirPlanning(car, ball);
    }

    List<AerialLookUp.AerialInfo> aerialInfos = AerialLookUp.getInfos(car.boost, ball.position.z);

    // TODO: Given a list of time to height based on boost angle / boost used.
    // Filter those that take too long / travel too far.
    // See either if:
    // 1) Do we have time / boost to turn and then jump immediately
    // 2) Check the first and last plans
    //   a) Can we travel to height but not far enough (break first then boost up)
    //   b) Can we travel to height and too far (not entirely sure how to do this one)

    // Path is turn + straight
    BallData groundBall = BallData.builder()
        .setVelocity(ball.velocity)
        .setPosition(ball.position.setZ(Constants.BALL_RADIUS))
        .setTime(ball.time)
        .build();
    Path oneTurn = PathPlanner.oneTurn(car, groundBall);
    if (oneTurn == null) {
      return Optional.empty();
    }

    Optional<AerialLookUp.AerialInfo> successfulAerial = aerialInfos.stream()
        .filter(aerialInfo -> works(aerialInfo, oneTurn, car, ball))
        .findFirst();

    if (successfulAerial.isPresent()) {
      return Optional.of(
          Plan.builder()
              .setTacticType(Tactic.TacticType.AERIAL)
              .setPath(oneTurn)
              .build(ball.time - car.elapsedSeconds));
    } else {
      return Optional.empty();
    }
  }

  private static boolean works(AerialLookUp.AerialInfo aerialInfo, Path oneTurn, CarData car, BallData ball) {
    // Estimate boost angle and back to flat with 2 radians per second
    double rotationTime = aerialInfo.boostAngle * .3;

    // TODO: This is an over estimate since the time to apex + rotation time is duplicate.
    // Estimate time needed in the air based on aerial + jump + rotation time
    double jumpTime = aerialInfo.time + rotationTime;

    // Calculates duration of maneuver
    double timeToImpact = ball.time - car.elapsedSeconds;

    // Calculate time until we must jump
    double timeToJump = timeToImpact - jumpTime;

    // Determines how much boost we can use on the ground.
    double boostReserve = car.boost - aerialInfo.boostUsed;

    double forwardSpeed = car.orientation.localCoordinates(car.velocity).x;

    // Given the time we have before we have to jump, determine how fast we can accelerate
    Accels.AccelResult maxAccelResult =
        Accels.accelerateForTime(forwardSpeed, timeToJump, boostReserve);

    // Max distance on the (ground + distance in the air) / time
    double maxHorizontalDistance
        = (maxAccelResult.getDistanceCovered() + (maxAccelResult.getEndSpeed() * jumpTime) + aerialInfo.horizontalTravel);

    // If we have time before we have to jump
    // And the distance we would have to travel on the ground is travelable
    // and we have enough boost to get there.
    return timeToJump > 0
        && maxHorizontalDistance > oneTurn.length()
        && car.boost >= aerialInfo.boostUsed + maxAccelResult.getBoostUsed();
  }

  private static Optional<Plan> inAirPlanning(CarData car, BallData ball) {
    Pair<FlightPlan, FlightLog> plan =
        makePlan(car, Moment.from(ball), ball.position.plus(ball.position.minus(car.position).toMagnitude(100)));

    FlightPlan flightPlan = plan.getFirst();
    if (flightPlan.isHittable()) {
      return Optional.of(Plan.builder()
          .setTacticType(Tactic.TacticType.AERIAL)
          .setBoostUsed(flightPlan.boostRequirement())
          .setAerialPlan(plan)
          .build(ball.time - car.elapsedSeconds));
    }

    return Optional.empty();
  }

  private final boolean freestyle = false;

  private void humanExecution(DataPacket input, Controls output, FlightPlan plan, FlightLog second) {

    bot.botRenderer.renderTarget(Color.GREEN, plan.setupLocation);

    FlightTrajectory trajectory = computeFlightTrajectory(input, plan);
    boolean isSetupPhase = plan.isSetupPhase(trajectory, input.car);
    Vector3 setupDeviation = plan.setUpDeviation(trajectory);
    Vector3 terminalDeviation = plan.terminalDeviation(trajectory);
    float timeToImpact = plan.interceptTime - input.car.elapsedSeconds;

    Vector3 carTarget = input.ball.position.minus(input.car.position);
    double targetNoseAngle = carTarget.angle(input.car.orientation.getNoseVector());
    if (isSetupPhase) {
      bot.botRenderer.setBranchInfo("Aerial setup");
      Angles3.pointAnyDirection(input.car, setupDeviation, output);
    } else if (terminalDeviation.magnitude() > 20 && terminalDeviation.angle(input.car.orientation.getNoseVector()) > .01) {
      bot.botRenderer.setBranchInfo("Target approach");
      Angles3.pointAnyDirection(input.car, terminalDeviation, output);
    } else if (timeToImpact * 2 < targetNoseAngle) {
      bot.botRenderer.setBranchInfo("Final alignment");
      Angles3.pointAnyDirection(input.car, carTarget, output);
    } else if (freestyle) {
      bot.botRenderer.setBranchInfo("Freestyle");
      output.withRoll(1);
    }

    Vector3 trackedAngle = isSetupPhase ? setupDeviation : terminalDeviation;

    double rotationalOffset = input.car.angularVelocity.angle(trackedAngle);

    if (trackedAngle.magnitude() > 20) {
      double sensitivity = .15;
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

  private static Pair<FlightPlan, FlightLog> makePlan(CarData car, Moment target, Vector3 targetResult) {
    Vector3 interceptLocation = computeInterceptLocation(car, target, targetResult);

    // TODO: Why are there arbitrary .25fs in here? I think maybe this is to account for the fast aerial?
    float flightDuration = target.time - car.elapsedSeconds;
    float startBoostTime = flightDuration * .25f;
    Vector3 boostVector = calculateBoost(car, interceptLocation, flightDuration, startBoostTime);

    return Pair.of(
        new FlightPlan(interceptLocation, car.elapsedSeconds, flightDuration, target, boostVector),
        new FlightLog(car.elapsedSeconds + flightDuration * .25f, boostVector));
  }

  /**
   * This method tells us the average boost vector given a start and end location.
   * The returned vector can be used to tell if we have enough thrust to get there.
   */
  private static Vector3 calculateBoost(CarData car, Vector3 interceptLocation, double flightDuration, double boostTime) {
    double startBoostTime = flightDuration - boostTime; // TODO: Why is this called startBoostTime when boostTime is called startBoostTime in the caller?

    double coefficient = 2 / (flightDuration * (flightDuration - (2 * startBoostTime))); // TODO: What are these 2s? Is this static?
    return interceptLocation
        .minus(car.position)
        .minus(car.velocity.multiply(flightDuration))
        .minus(Vector3.of(0, 0, Constants.NEG_GRAVITY).multiply(.5 * (flightDuration * flightDuration)))
        .multiply(coefficient);
  }

  private static Vector3 computeInterceptLocation(CarData car, Moment moment, Vector3 targetPosition) {
    if (moment.type == Moment.Type.BALL) {
      // Assume that we hit the ball hard (~2200) and aim for the top.
      Vector3 ballToTarget = targetPosition.minus(moment.position);
      double flatDistance = ballToTarget.flatten().magnitude();
      double travelTime = flatDistance / 2200;
      double verticalDrop = Accels.verticalDistance(moment.velocity.z, travelTime);

      // Pretend the target is higher than it is.
      Vector3 adjustedTarget = targetPosition.addZ(-verticalDrop);

      // Determine the velocity sheer between the ball and the intended target.
      Orientation contactOrientation = Orientation.fromFlatVelocity(ballToTarget);
      Vector3 relativeVelocity = contactOrientation.localCoordinates(moment.velocity);
      adjustedTarget = adjustedTarget.addX(Math.signum(contactOrientation.getNoseVector().y) * relativeVelocity.y * 1);

      // Determine the sheer between the car and the intended target.
      Orientation carContactOrientation = Orientation.fromFlatVelocity(targetPosition.minus(car.position));
      Vector3 carRelativeVelocity = carContactOrientation.localCoordinates(car.velocity);
//      adjustedTarget = adjustedTarget.addY(Math.signum(carContactOrientation.getNoseVector().y) * -carRelativeVelocity.y * 100);

      // TODO: This should actually be the angle between the ball velocity and the car velocity
      double ballRadiusAdjustment = 1 + (Math.abs(carRelativeVelocity.y) * .4 / 500);
      ballRadiusAdjustment = Numbers.clamp(ballRadiusAdjustment, 1, 1.6);

      Vector3 newTargetToBall = moment.position.minus(adjustedTarget);

      return newTargetToBall
          .toMagnitudeUnchecked(Constants.BALL_RADIUS * ballRadiusAdjustment)
          .plus(moment.position);
    } else {
      return moment.position;
    }
  }

  /**
   * A class which stores the intended flight path including intercept place and time and setup vector.
   */
  public static class FlightPlan {
    final float interceptTime;
    final float flightDuration;
    final Vector3 interceptLocation;
    final Moment target;
    final Vector3 averageBoostVector;
    final Vector3 setupLocation;

    public FlightPlan(
        Vector3 interceptLocation,
        float currentTime,
        float flightDuration,
        Moment target,
        Vector3 averageBoostVector) {

      this.interceptLocation = interceptLocation;
      this.target = target;
      this.averageBoostVector = averageBoostVector;
      this.interceptTime = currentTime + flightDuration;
      this.flightDuration = flightDuration;
      Vector3 setupOffset = interceptLocation.minus(target.position)
          .toMagnitudeUnchecked(Numbers.clamp(flightDuration, 0, 1) * 250);
      setupLocation = interceptLocation.plus(setupOffset);
    }

    public Vector3 setUpDeviation(FlightTrajectory trajectory) {
      return setupLocation.minus(trajectory.flightTerminus);
    }

    public Vector3 terminalDeviation(FlightTrajectory trajectory) {
      return interceptLocation.minus(trajectory.flightTerminus);
    }

    public boolean isSetupPhase(FlightTrajectory trajectory, CarData car) {
      double distanceToSetup = setupLocation.distance(trajectory.flightTerminus);
      double distanceToTerminus = interceptLocation.distance(trajectory.flightTerminus);
      return distanceToSetup > 20
          && distanceToSetup < distanceToTerminus
          && hasAccelerationMargin()
          && boostRequirement() * .7 < car.boost;
    }

    public boolean isHittable() {
      return averageBoostVector.magnitude() < Constants.BOOSTED_ACCELERATION * .95;
    }

    public boolean hasAccelerationMargin() {
      return averageBoostVector.magnitude() < Constants.BOOSTED_ACCELERATION * .7;
    }

    public double boostRequirement() {
      return averageBoostVector.magnitude() * flightDuration / Constants.BOOSTED_ACCELERATION;
    }
  }

  /**
   * Manages the actual flying controls including when to feather the boost.
   */
  public static class FlightLog {
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
  }

  /** Tracks the path of the flight given no additional inputs. */
  private static class FlightTrajectory {

    private final Vector3 flightTerminus;
    private final ImmutableList<CarPrediction.PredictionNode> flightPath;

    public FlightTrajectory(ImmutableList<CarPrediction.PredictionNode> flightPath) {
      this.flightTerminus = Iterables.getLast(flightPath).position;
      this.flightPath = flightPath;
    }
  }
}
