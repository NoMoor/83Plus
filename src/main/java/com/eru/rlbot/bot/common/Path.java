package com.eru.rlbot.bot.common;

import static com.eru.rlbot.bot.common.Constants.STEP_SIZE;

import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Objects;

public class Path {

  private final ImmutableList<Segment> nodes;
  private final CarData start;
  private final CarData target;

  private int currentIndex;
  private boolean isTimed;

  private Path(CarData car, CarData targetCar, ImmutableList<Segment> nodes) {
    this.nodes = nodes;
    this.start = car;
    this.target = targetCar;
    this.currentIndex = 0;
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

  public void startTiming(DataPacket input) {
    // TODO: Adjust for acceleration / deceleration segments.
    traverseTime(0, input.car.boost, true);
  }

  public static final float LEAD_TIME = .05f;

  public Vector3 getCurrentTarget(DataPacket input) {
    float targetTime = input.car.elapsedSeconds + LEAD_TIME;

    Segment segment = nodes.get(currentIndex);
    if (hasNextSegment() && segment.endTime < targetTime) {
      segment.markComplete();
      segment = nodes.get(currentIndex++);
    }

    // Clamp to the end of the last target.
    double segmentCompletion =
        Math.min((targetTime - segment.startTime) / (segment.endTime - segment.startTime), 1);

    return segment.getProgress(segmentCompletion);
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

  public static final double SLOWING_BUFFER = -20;

  public double minimumTraverseTime() {
    return traverseTime(currentIndex, start.boost, false);
  }

  public double nonBoostingTraverseTime() {
    return traverseTime(currentIndex, 0, false);
  }

  private double traverseTime(int startIndex, double boostAmount, boolean tagSegments) {
    double boost = boostAmount;
    double currentVelocity = start.groundSpeed;
    double time = 0;

    ListIterator<Segment> segmentIterator = nodes.listIterator(startIndex);
    while (segmentIterator.hasNext()) {
      Segment nextSegment = segmentIterator.next();
      nextSegment.startTime = start.elapsedSeconds + time;
      double speedGoal = target.groundSpeed;
      if (segmentIterator.hasNext()) {
        speedGoal = nodes.get(segmentIterator.nextIndex()).maxSpeed();
      }

      double segmentDistance = nextSegment.flatDistance();

      while (segmentDistance > 0) {
        double nextAcceleration = 0;
        boolean canGoFaster = canGoFaster(currentVelocity, nextSegment, speedGoal);

        if (canGoFaster) {
          nextAcceleration = Accels.acceleration(currentVelocity) + ((boost > 0) ? Constants.BOOSTED_ACCELERATION : 0) * .9;

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
      }
      nextSegment.endTime = start.elapsedSeconds + time;
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
    return canGoFaster(currentVelocity, nodes.get(currentIndex), getSpeedGoal());
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

  private static final float COMPLETION_DISTANCE = 50;
  public Segment getSegment(DataPacket input) {
    Segment nextSegment = nodes.get(currentIndex);
    if (hasNextSegment() && nextSegment.end.distance(input.car.position) < COMPLETION_DISTANCE) {
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

  public static class Segment {
    public final Vector3 start;
    public final Vector3 end;
    public final Type type;
    public final Circle circle;
    public final boolean clockWise;

    public double endTime;
    public double startTime;
    private boolean isComplete;

    public static Segment arc(Vector3 start, CarData target, Circle circle) {
      return new Segment(start, target, circle);
    }

    public static Segment straight(Vector3 start, Vector3 end) {
      return new Segment(start, end, Type.STRAIGHT);
    }

    // TODO: Add speed?
    public static Segment jump(Vector3 start, Vector3 end) {
      return new Segment(start, end, Type.JUMP);
    }

    private Segment(Vector3 start, CarData exitCar, Circle circle) {
      this.start = start;
      this.end = exitCar.position;
      this.circle = circle;
      this.clockWise = circle.center.minus(exitCar.position).cross(exitCar.orientation.getNoseVector()).z < 0;
      this.type = Type.ARC;
    }

    private Segment(Vector3 start, Vector3 end, Type type) {
      this.start = start;
      this.end = end;
      this.type = type;
      this.circle = null;
      this.clockWise = false;
    }

    private void markComplete() {
      this.isComplete = true;
    }

    public boolean isComplete() {
      return this.isComplete;
    }

    private double maxSpeed() {
      switch (type) {
        case STRAIGHT:
        case JUMP:
          return Constants.BOOSTED_MAX_SPEED;
        case ARC:
          return circle.maxSpeed;
        default:
          throw new IllegalStateException(String.format("Unsupported type %s", type));
      }
    }

    public Vector3 getProgress(double segmentCompletion) {
      switch (type) {
        case ARC:
          double radians = getRadians() * segmentCompletion;
          double radianOffset = Vector2.WEST.correctionAngle(start.minus(circle.center).flatten());

          return Circle.pointOnCircle(circle.center, circle.radius, radians + radianOffset);
        case STRAIGHT:
        case JUMP:
          return end.minus(start).multiply(segmentCompletion).plus(start);
        default:
          throw new IllegalStateException("Doh!");
      }
    }

    public enum Type {
      STRAIGHT,
      ARC,
      JUMP
    }

    public double getRadians() {
      Objects.requireNonNull(circle);

      Vector3 legA = circle.center.minus(start);
      Vector3 legB = circle.center.minus(end);

      double value = legA.flatten().correctionAngle(legB.flatten());

      // TODO: This is not tested...
      if (value < 0 && clockWise) {
        value += Math.PI * 2;
      } else if (value > 0 && !clockWise) {
        value -= Math.PI * 2;
      }
      return value;
    }

    private Double distance;

    private double flatDistance() {
      if (distance == null) {
        switch (type) {
          case STRAIGHT:
          case JUMP:
            distance = start.flatten().distance(end.flatten());
            break;
          case ARC:
            distance = calculateArcLength();
            break;
          default:
            throw new IllegalStateException(String.format("Type %s unsupported", type));
        }
      }

      return distance;
    }

    private double calculateArcLength() {
      return Math.abs(getRadians()) * circle.radius;
    }
  }
}
