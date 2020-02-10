package com.eru.rlbot.bot.common;

import static com.eru.rlbot.bot.common.Constants.STEP_SIZE;

import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.*;

public class Path {

  private static final Logger logger = LogManager.getLogger("Path");

  private ImmutableList<Segment> terseNodes;
  private ImmutableList<Segment> nodes;
  // Alternatively, we could move the target forward...
  private Segment extension;

  private final CarData start;
  private final CarData target;

  private int currentIndex;
  private boolean isTimed;
  private boolean isOffCourse;

  private Map<Pair<Double, Double>, Plan> planMap = new HashMap<>();

  private Path(CarData car, CarData targetCar, ImmutableList<Segment> nodes) {
    this.terseNodes = nodes;
    this.nodes = nodes;
    this.start = car;
    this.target = targetCar;
  }

  private Double distance;

  public Path(Builder builder) {
    this(builder.startingCar, builder.targetCar, ImmutableList.copyOf(builder.segments));
  }

  public double length() {
    if (distance == null) {
      distance = nodes.stream()
          .mapToDouble(Segment::flatDistance)
          .sum();
    }
    return distance;
  }

  public static Builder builder() {
    return new Builder();
  }

  public void lockAndSegment() {
    float targetTime = target.elapsedSeconds - start.elapsedSeconds;
    if (targetTime < .1) {
      return;
    }

    long nanoStartTime = System.nanoTime();
    Plan traversePlan = makeSpeedPlan(start.boost, targetTime);
    segmentByPlan(traversePlan);
    long nanoEndTime = System.nanoTime();
    logger.debug("Took {}ns to segment", (nanoEndTime - nanoStartTime));
  }

  static final float LEAD_FRAMES = 12f;
  static final float LEAD_TIME = LEAD_FRAMES / Constants.STEP_SIZE_COUNT;

  Vector3 pidTarget(DataPacket input) {
    updateSegmentIndex(input);
    return targetForTime(input.car.elapsedSeconds + LEAD_TIME);
  }

  Vector3 currentTarget(DataPacket input) {
    updateSegmentIndex(input);
    return targetForTime(input.car.elapsedSeconds);
  }

  private Vector3 targetForTime(float targetTime) {
    int index = currentIndex;
    while (nodes.size() - 1 > index && nodes.get(index).endTime < targetTime) {
      index++;
    }
    Segment segment = nodes.get(index);

    if (segment.endTime < targetTime && extension != null) {
      segment = extension;
    }

    // Clamp to the end of the last target.
    double segmentCompletion =
        Math.min((targetTime - segment.startTime) / (segment.endTime - segment.startTime), 1);

    return segment.getProgress(segmentCompletion);
  }

  private void updateSegmentIndex(DataPacket input) {
    while (hasNextSegment() && nodes.get(currentIndex).endTime < input.car.elapsedSeconds) {
      nodes.get(currentIndex++).markComplete();
    }
  }

  public void extendThroughBall() {
    Segment lastSegment = nodes.get(nodes.size() - 1);
    this.extension = lastSegment.extend(Path.LEAD_TIME * target.velocity.magnitude());
  }

  public void markOffCourse() {
    this.isOffCourse = true;
  }

  public boolean isOffCourse() {
    return this.isOffCourse;
  }

  public CarData getTarget() {
    return target;
  }

  public ImmutableList<Segment> allTerseNodes() {
    if (extension != null) {
      return ImmutableList.<Segment>builder()
          .addAll(terseNodes)
          .add(extension)
          .build();
    }
    return terseNodes;
  }

  public static final class Builder {

    private LinkedList<Segment> segments = new LinkedList<>();
    private CarData startingCar;
    private CarData targetCar;

    public Builder setStartingCar(CarData startingCar) {
      this.startingCar = startingCar;
      return this;
    }

    public Builder setTargetCar(CarData targetCar) {
      this.targetCar = targetCar;
      return this;
    }

    public Builder addEarlierSegment(Segment segment) {
      segments.addFirst(segment);
      return this;
    }

    public Builder addEarlierSegments(ImmutableList<Segment> shortestBiArc) {
      shortestBiArc.reverse().forEach(segments::addFirst);
      return this;
    }

    public Path build() {
      return new Path(this);
    }
  }

  private static final double SLOWING_BUFFER = 100;

  double minimumTraverseTime() {
    return fastestTraverseTime(currentIndex, start.boost).traverseTime;
  }

  double nonBoostingTraverseTime() {
    return fastestTraverseTime(currentIndex, 0).traverseTime;
  }

  private static final float ACCELERATION_BUFFER = 1f;
  private static final double BOOST_DELTA = .275;

  @VisibleForTesting
  Plan makeSpeedPlan(double boostAmount, double targetTime) {
    // TODO: Remove this from production code.
    Plan boostingPlan = fastestTraverseTime(0, boostAmount);
    Preconditions.checkState(
        targetTime >= boostingPlan.traverseTime,
        "Should be time under %s but was %s", targetTime, boostingPlan.traverseTime);

    Plan workingPlan = fastestTraverseTime(0, 0);

    if (workingPlan.traverseTime < targetTime) {
      return nonBoostingPlan(boostAmount, targetTime);
    } else {
      return boostingPlan(workingPlan, boostAmount, targetTime);
    }
  }

  private static final double COASTING_TIME_GRANULARITY = 0.01;
  private static final double BREAKING_SPEED_GRANULARITY = 5;

  Plan nonBoostingPlan(double boostAmount, double targetTime) {
    double minBreakingSpeed = 0;
    double maxBreakingSpeed = start.velocity.magnitude();
    double targetBreakingSpeed = (minBreakingSpeed + maxBreakingSpeed) / 2;

    // Need to break
    Accels.AccelResult breaking = Accels.distanceToSlow(start.velocity.magnitude(), targetBreakingSpeed);
    double remainingDistance = length() - breaking.distance;
    Accels.AccelResult striking = Accels.boostedTimeToDistance(boostAmount, targetBreakingSpeed, remainingDistance);
    double coastingTime = targetTime - breaking.time - striking.time;

    while (Math.abs(coastingTime) < COASTING_TIME_GRANULARITY
        && (minBreakingSpeed < targetBreakingSpeed - BREAKING_SPEED_GRANULARITY
        && maxBreakingSpeed + BREAKING_SPEED_GRANULARITY < maxBreakingSpeed)) {

      if (coastingTime < 0) {
        // Breaking too much
        minBreakingSpeed = targetBreakingSpeed;
      } else {
        maxBreakingSpeed = targetBreakingSpeed;
      }

      targetBreakingSpeed = (minBreakingSpeed + maxBreakingSpeed) / 2;

      breaking = Accels.distanceToSlow(start.velocity.magnitude(), targetBreakingSpeed);
      remainingDistance = length() - breaking.distance;
      striking = Accels.boostedTimeToDistance(boostAmount, targetBreakingSpeed, remainingDistance);
      coastingTime = targetTime - breaking.time - striking.time;
    }

    Plan.Builder planBuilder = Plan.builder().setPath(this);
    double breakingTime = breaking.time;
    while (breakingTime > 0) {
      planBuilder.addThrottleInput(false, -1);
      breakingTime -= STEP_SIZE;
    }

    while (coastingTime > 0) {
      planBuilder.addThrottleInput(false, .02);
      coastingTime -= STEP_SIZE;
    }

    double accelerationTime = striking.time;
    double boostUsed = striking.boost;
    while (accelerationTime > 0) {
      planBuilder.addThrottleInput(boostUsed > 0, 1);
      accelerationTime -= STEP_SIZE;
      boostAmount -= Constants.BOOST_RATE / STEP_SIZE;
    }

    Plan plan = planBuilder
        .setBoostUsed(striking.boost)
        .build(targetTime);

    planMap.put(Pair.of(boostAmount, targetTime), plan);

    return plan;
  }

  private Plan boostingPlan(Plan nonBoostingPlan, double boostAmount, double targetTime) {
    Plan workingPlan = nonBoostingPlan;
    double maxBoost = boostAmount;
    double minBoost = 0;
    double searchBoostAmount = ((maxBoost + minBoost) / 2);

    double timeDiff = Math.abs(workingPlan.traverseTime - targetTime);

    boolean searchConverged = (searchBoostAmount - BOOST_DELTA < minBoost)
        || (searchBoostAmount + BOOST_DELTA > maxBoost);
    while (timeDiff > .0166 && !searchConverged) {

      Plan tempPlan = fastestTraverseTime(0, searchBoostAmount);
      if (tempPlan.traverseTime > targetTime) {
        minBoost = searchBoostAmount;
      } else {
        maxBoost = searchBoostAmount;
      }

      // Update variables
      workingPlan = tempPlan;
      searchBoostAmount = ((maxBoost + minBoost) / 2); // This will not overflow.
      timeDiff = Math.abs(workingPlan.traverseTime - targetTime);
      searchConverged = (searchBoostAmount - BOOST_DELTA < minBoost)
          || (searchBoostAmount + BOOST_DELTA > maxBoost);

    }

    planMap.put(Pair.of(boostAmount, targetTime), workingPlan);

    return workingPlan;
  }

  @VisibleForTesting
  Plan fastestTraverseTime(int startIndex, double startingBoost) {
    Plan.Builder planBuilder = Plan.builder()
        .setPath(this);

    double boostRemaining = startingBoost;
    double currentVelocity = start.groundSpeed;
    double time = 0;

    ListIterator<Segment> segmentIterator = nodes.listIterator(startIndex);
    while (segmentIterator.hasNext()) {
      Segment segment = segmentIterator.next();

      double speedTarget = target.groundSpeed;
      if (segmentIterator.hasNext()) {
        speedTarget = nodes.get(segmentIterator.nextIndex()).maxSpeed();
      }

      double segmentDistance = segment.flatDistance();

      while (segmentDistance > 0) {
        double nextAcceleration;
        boolean canGoFaster = canGoFaster(currentVelocity, segment.maxSpeed(), speedTarget, segmentDistance, segment.type);

        boolean isBoosting = false;
        double throttle;

        if (canGoFaster) {
          isBoosting = boostRemaining > 0;
          throttle = 1.0;
          nextAcceleration = Accels.acceleration(currentVelocity) * ACCELERATION_BUFFER
              + (isBoosting ? Constants.BOOSTED_ACCELERATION : 0);

          if (isBoosting) {
            boostRemaining -= STEP_SIZE * 33;
          }
        } else if (breakNow(currentVelocity, segmentDistance, speedTarget)) {
          throttle = -1;
          nextAcceleration = -Constants.BREAKING_DECELERATION;
        } else {
          throttle = .02f;
          nextAcceleration = Accels.acceleration(currentVelocity) * .02;
        }

        double newVelocity = currentVelocity + (nextAcceleration * STEP_SIZE);
        segmentDistance -= ((currentVelocity + newVelocity) / 2) * STEP_SIZE;
        currentVelocity = newVelocity;
        time += STEP_SIZE;
        planBuilder.addThrottleInput(isBoosting, throttle);
      }
    }

    Plan plan = planBuilder
        .setBoostUsed(startingBoost - boostRemaining)
        .build(time);

    planMap.put(Pair.of(startingBoost, Double.MAX_VALUE), plan);

    return plan;
  }

  private void segmentByPlan(Plan traversePlan) {
    Iterator<Plan.ThrottleInput> inputs = traversePlan.throttleInputList.iterator();
    ImmutableList.Builder<Segment> timedSegments = ImmutableList.builder();

    double currentVelocity = start.groundSpeed;
    double time = 0;

    ListIterator<Segment> segmentIterator = nodes.listIterator();
    while (segmentIterator.hasNext()) {
      Segment nextSegment = segmentIterator.next();
      nextSegment.startTime = start.elapsedSeconds + time;

      double segmentDistance = nextSegment.flatDistance();
      double segmentStartSpeed = currentVelocity;

      while (segmentDistance > 0) {
        double nextAcceleration = 0;

        if (inputs.hasNext()) {
          nextAcceleration = getAcceleration(currentVelocity, inputs.next());
        }

        double newVelocity = currentVelocity + (nextAcceleration * STEP_SIZE);
        segmentDistance -= ((currentVelocity + newVelocity) / 2) * STEP_SIZE;
        currentVelocity = newVelocity;
        time += STEP_SIZE;

        // Split if significant acceleration
        if (Math.abs(segmentStartSpeed - currentVelocity) > Accels.acceleration(segmentStartSpeed) / 4) {
          double traveledSegmentDistance = nextSegment.flatDistance() - segmentDistance;
          Pair<Segment, Segment> segments =
              nextSegment.splitSegment(traveledSegmentDistance, start.elapsedSeconds + time);

          timedSegments.add(segments.getFirst());
          nextSegment = segments.getSecond();
          segmentDistance = nextSegment.flatDistance();
          segmentStartSpeed = currentVelocity;
        }

      }

      nextSegment.endTime = start.elapsedSeconds + time;
      timedSegments.add(nextSegment);
    }

    nodes = timedSegments.build();
  }

  private double getAcceleration(double currentVelocity, Plan.ThrottleInput input) {
    if (input.boost) {
      return Accels.acceleration(currentVelocity) + Constants.BOOSTED_ACCELERATION;
    } else if (input.throttle > 0) {
      return Accels.acceleration(currentVelocity) * input.throttle;
    } else if (input.throttle == 0) {
      return Constants.COASTING_DECELERATION;
    } else {
      return Constants.BREAKING_DECELERATION;
    }
  }

  public void setTimed(boolean isTimed) {
    this.isTimed = isTimed;
  }

  private boolean canGoFaster(double currentVelocity, double segmentSpeedLimit, double endSegmentSpeedTarget, double segmentRemainingDistance, Segment.Type type) {
    if (type == Segment.Type.JUMP) {
      return false;
    }

    if (segmentSpeedLimit <= currentVelocity) {
      return false;
    }

    Accels.AccelResult slowingDistance = Accels.distanceToSlow(currentVelocity, endSegmentSpeedTarget);
    return slowingDistance.distance + SLOWING_BUFFER < segmentRemainingDistance;
  }

  public Segment getSegment(DataPacket input) {
    Segment nextSegment = nodes.get(currentIndex);
    if (hasNextSegment() && nextSegment.endTime < input.car.elapsedSeconds) {
      nextSegment.markComplete();
      currentIndex++;
    }

    return nodes.get(currentIndex);
  }

  int getCurrentIndex() {
    return currentIndex;
  }

  private boolean hasNextSegment() {
    return nodes.size() > currentIndex + 1;
  }

  public double getSpeedGoal() {
    int nextIndex = currentIndex + 1;
    return nodes.size() > nextIndex ? nodes.get(nextIndex).maxSpeed() : target.groundSpeed;
  }

  ImmutableList<Segment> allNodes() {
    if (extension != null) {
      ImmutableList.Builder<Segment> nodeListBuilder = ImmutableList.builder();
      return nodeListBuilder.addAll(nodes).add(extension).build();
    }

    return nodes;
  }

  private boolean breakNow(double groundSpeed, double distance, double goalSpeed) {
    if (Math.abs(groundSpeed - goalSpeed) < 50) {
      return false;
    }

    double slowingDistance = Accels.distanceToSlow(groundSpeed, goalSpeed).distance;
    return slowingDistance > 0 && slowingDistance + SLOWING_BUFFER > distance;
  }

  public float getEndTime() {
    return target.elapsedSeconds;
  }
}
