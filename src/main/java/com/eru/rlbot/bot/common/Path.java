package com.eru.rlbot.bot.common;

import static com.eru.rlbot.bot.common.Constants.STEP_SIZE;

import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;
import java.util.LinkedList;
import java.util.ListIterator;

public class Path {

  private ImmutableList<Segment> nodes;
  // Alternatively, we could move the target forward...
  private Segment extension;

  private final CarData start;
  private final CarData target;

  private int currentPidIndex;
  private int currentIndex;
  private boolean isTimed;
  private boolean isOffCourse;

  private Path(CarData car, CarData targetCar, ImmutableList<Segment> nodes) {
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

  public void lockAndSegment(DataPacket input) {
    traverseTime(0, input.car.boost, true);
  }

  static final float LEAD_FRAMES = 7f;
  static final float LEAD_TIME = LEAD_FRAMES / Constants.STEP_SIZE_COUNT;

  Vector3 updateAndGetPidTarget(DataPacket input) {
    float targetTime = input.car.elapsedSeconds + LEAD_TIME;

    updateSegmentIndex(input);

    while (hasNextPidSegment() && nodes.get(currentPidIndex).endTime < targetTime) {
      currentPidIndex++;
    }
    Segment segment = nodes.get(currentPidIndex);

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

    public Path build() {
      return new Path(this);
    }
  }

  private static final double SLOWING_BUFFER = 100;

  double minimumTraverseTime() {
    return traverseTime(currentIndex, start.boost, false);
  }

  double nonBoostingTraverseTime() {
    return traverseTime(currentIndex, 0, false);
  }

  private static final float ACCELERATION_BUFFER = .95f;

  private double traverseTime(int startIndex, double boostAmount, boolean modifySegments) {
    ImmutableList.Builder<Segment> timedSegments = ImmutableList.builder();

    double boost = boostAmount;
    double currentVelocity = start.groundSpeed;
    double time = 0;

    ListIterator<Segment> segmentIterator = nodes.listIterator(startIndex);
    while (segmentIterator.hasNext()) {
      Segment nextSegment = segmentIterator.next();

      if (modifySegments) {
        nextSegment.startTime = start.elapsedSeconds + time;
      }

      double speedGoal = target.groundSpeed;
      if (segmentIterator.hasNext()) {
        speedGoal = nodes.get(segmentIterator.nextIndex()).maxSpeed();
      }

      double segmentDistance = nextSegment.flatDistance();
      double segmentStartSpeed = currentVelocity;

      while (segmentDistance > 0) {
        double nextAcceleration = 0;
        boolean canGoFaster = canGoFaster(currentVelocity, nextSegment, speedGoal);

        if (canGoFaster) {
          nextAcceleration = Accels.acceleration(currentVelocity) * ACCELERATION_BUFFER
              + ((boost > 0) ? Constants.BOOSTED_ACCELERATION : 0);

          if (boost > 0) {
            boost -= STEP_SIZE * 33;
          }
        } else if (breakNow(currentVelocity, segmentDistance, speedGoal)) {
          nextAcceleration = -Constants.BREAKING_DECELERATION;
        }

        double newVelocity = currentVelocity + (nextAcceleration * STEP_SIZE);
        segmentDistance -= ((currentVelocity + newVelocity) / 2) * STEP_SIZE;
        currentVelocity = newVelocity;
        time += STEP_SIZE;

        // Split if significant acceleration
        if (modifySegments && Math.abs(segmentStartSpeed - currentVelocity) > Accels.acceleration(segmentStartSpeed) / 4) {
          double traveledSegmentDistance = nextSegment.flatDistance() - segmentDistance;
          Pair<Segment, Segment> segments =
              nextSegment.splitSegment(traveledSegmentDistance, start.elapsedSeconds + time);

          timedSegments.add(segments.getFirst());
          nextSegment = segments.getSecond();
          segmentDistance = nextSegment.flatDistance();
          segmentStartSpeed = currentVelocity;
        }

      }

      if (modifySegments) {
        nextSegment.endTime = start.elapsedSeconds + time;
        timedSegments.add(nextSegment);
      }
    }

    if (modifySegments) {
      nodes = timedSegments.build();
    }

    return time;
  }

  public void setTimed(boolean isTimed) {
    this.isTimed = isTimed;
  }

  public boolean isTimed() {
    return isTimed;
  }

  public boolean canGoFaster(double currentVelocity) {
    return canGoFaster(currentVelocity, nodes.get(currentPidIndex), getSpeedGoal());
  }

  private boolean canGoFaster(double currentVelocity, Segment segment, double maxSpeed) {
    if (segment.type == Segment.Type.JUMP) {
      return false;
    }

    double segmentDistance = segment.flatDistance();
    double segmentSpeedLimit = segment.maxSpeed();

    return segmentSpeedLimit > currentVelocity
        && Accels.distanceToSlow(currentVelocity, maxSpeed) + SLOWING_BUFFER < segmentDistance;
  }

  // TODO: This doesn't work.
  private static final float COMPLETION_DISTANCE = 50;
  public Segment getSegment(DataPacket input) {
    Segment nextSegment = nodes.get(currentIndex);
    if (hasNextSegment() && nextSegment.end.distance(input.car.position) < COMPLETION_DISTANCE) {
      nextSegment.markComplete();
      currentIndex++;
    }

    return nodes.get(currentPidIndex);
  }

  int getCurrentPidIndex() {
    return currentPidIndex;
  }

  int getCurrentIndex() {
    return currentIndex;
  }

  private boolean hasNextSegment() {
    return nodes.size() > currentIndex + 1;
  }

  private boolean hasNextPidSegment() {
    return nodes.size() > currentPidIndex + 1;
  }

  public double getSpeedGoal() {
    int nextIndex = currentPidIndex + 1;
    return nodes.size() > nextIndex ? nodes.get(nextIndex).maxSpeed() : target.groundSpeed;
  }

  ImmutableList<Segment> allNodes() {
    if (extension != null) {
      ImmutableList.Builder<Segment> nodeListBuilder = ImmutableList.builder();
      return nodeListBuilder.addAll(nodes).add(extension).build();
    }

    return nodes;
  }

  public boolean breakNow(CarData car) {
    return breakNow(car.groundSpeed, car.position.distance(nodes.get(currentIndex).end), getSpeedGoal());
  }

  private boolean breakNow(double groundSpeed, double distance, double goalSpeed) {
    double slowingDistance = Accels.distanceToSlow(groundSpeed, goalSpeed);
    return slowingDistance > 0 && slowingDistance + SLOWING_BUFFER > distance;
  }

  public float getEndTime() {
    return target.elapsedSeconds;
  }
}
