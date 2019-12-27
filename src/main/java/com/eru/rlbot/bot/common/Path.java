package com.eru.rlbot.bot.common;

import static com.eru.rlbot.bot.common.Constants.STEP_SIZE;

import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;
import java.util.ListIterator;
import java.util.Objects;

public class Path {

  private final CarData targetCar;
  private final ImmutableList<Segment> nodes;
  private final CarData car;

  private boolean isTimed;

  public Path(CarData car, CarData targetCar, Segment... nodes) {
    this(car, targetCar, ImmutableList.copyOf(nodes));
  }

  public Path(CarData car, CarData targetCar, ImmutableList<Segment> nodes) {
    this.nodes = nodes;
    this.car = car;
    this.targetCar = targetCar;
  }

  private Double distance;

  public double length() {
    if (distance == null) {
      distance = nodes.stream()
          .mapToDouble(Segment::distance)
          .sum();
    }
    return distance;
  }

  public static final double SLOWING_BUFFER = -20;

  public double minimumTraverseTime() {
    return traverseTime(car.boost);
  }

  public double nonBoostingTraverseTime() {
    return traverseTime(0);
  }

  private double traverseTime(double boostAmount) {
    double boost = boostAmount;
    double currentVelocity = car.groundSpeed;
    double time = 0;

    ListIterator<Segment> segmentIterator = nodes.listIterator();
    while (segmentIterator.hasNext()) {
      Segment nextSegment = segmentIterator.next();
      double speedGoal = targetCar.groundSpeed;
      if (segmentIterator.hasNext()) {
        speedGoal = nodes.get(segmentIterator.nextIndex()).maxSpeed();
      }

      double segmentDistance = nextSegment.distance();

      while (segmentDistance > 0) {
        double nextAcceleration = 0;
        boolean canGoFaster = canGoFaster(currentVelocity, nextSegment, speedGoal);

        if (canGoFaster) {
          nextAcceleration = Accels.acceleration(currentVelocity) + ((boost > 0) ? Constants.BOOSTED_ACCELERATION : 0);

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
    return canGoFaster(currentVelocity, nodes.get(0), getSpeedGoal());
  }

  private boolean canGoFaster(double currentVelocity, Segment nextSegment, double maxSpeed) {
    double segmentDistance = nextSegment.distance();
    double segmentSpeedLimit = nextSegment.maxSpeed();

    return segmentSpeedLimit > currentVelocity
        && Accels.distanceToSlow(currentVelocity, maxSpeed) + SLOWING_BUFFER < segmentDistance;
  }

  public Segment getSegment(DataPacket input) {
    Segment nextSegment = nodes.get(0);
    return (nodes.size() > 1 && nextSegment.end.distance(input.car.position) < 20) ? nodes.get(1) : nextSegment;
  }

  public double getSpeedGoal() {
    return nodes.size() > 1 ? nodes.get(1).maxSpeed() : targetCar.groundSpeed;
  }

  public ImmutableList<Segment> allNodes() {
    return nodes;
  }

  public boolean breakNow(CarData car) {
    return breakNow(car.groundSpeed, car.position.distance(nodes.get(0).end), getSpeedGoal());
  }

  private boolean breakNow(double groundSpeed, double distance, double goalSpeed) {
    double slowingDistance = Accels.distanceToSlow(groundSpeed, goalSpeed);
    return slowingDistance > 0 && slowingDistance + SLOWING_BUFFER > distance;
  }

  public float getEndTime() {
    return targetCar.elapsedSeconds;
  }

  public static class Segment {
    public final Vector3 start;
    public final Vector3 end;
    public final Type type;
    public final Circle circle;
    public final boolean clockWise;

    public Segment(Vector3 start, CarData exitCar, Circle circle) {
      this.start = start;
      this.end = exitCar.position;
      this.circle = circle;
      this.clockWise = circle.center.minus(exitCar.position).cross(exitCar.orientation.getNoseVector()).z < 0;
      this.type = Type.ARC;
    }

    public Segment(Vector3 start, Vector3 end) {
      this.start = start;
      this.end = end;
      this.type = Type.STRAIGHT;
      this.circle = null;
      this.clockWise = false;
    }

    private double maxSpeed() {
      switch (type) {
        case STRAIGHT:
          return Constants.BOOSTED_MAX_SPEED;
        case ARC:
          return circle.maxSpeed;
        default:
          throw new IllegalStateException(String.format("Unsupported type %s", type));
      }
    }

    public enum Type {
      STRAIGHT,
      ARC
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

    private double distance() {
      if (distance == null) {
        switch (type) {
          case STRAIGHT:
            distance = start.distance(end);
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
