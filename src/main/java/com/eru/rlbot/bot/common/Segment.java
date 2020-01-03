package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;
import java.util.Objects;

public class Segment {

  public final Vector3 start;
  public final Vector3 end;
  public final Type type;
  public final Circle circle;
  public final boolean clockWise;

  double endTime;
  double startTime;
  private boolean isComplete;

  public static Segment arc(Vector3 start, Vector3 end, Circle circle, boolean clockWise) {
    return new Segment(start, end, circle, clockWise);
  }

  public static Segment straight(Vector3 start, Vector3 end) {
    return new Segment(start, end, Type.STRAIGHT);
  }

  // TODO: Add speed?
  public static Segment jump(Vector3 start, Vector3 end) {
    return new Segment(start, end, Type.JUMP);
  }

  private Segment(Vector3 start, CarData target, Circle circle) {
    this.start = start;
    this.end = target.position;
    this.circle = circle;
    this.clockWise = circle.isClockwise(target);
    this.type = Type.ARC;
  }


  public Segment(Vector3 start, Vector3 end, Circle circle, boolean clockWise) {
    this.start = start;
    this.end = end;
    this.circle = circle;
    this.clockWise = clockWise;
    this.type = Type.ARC;
  }

  private Segment(Vector3 start, Vector3 end, Type type) {
    this.start = start;
    this.end = end;
    this.type = type;
    this.circle = null;
    this.clockWise = false;
  }

  void markComplete() {
    this.isComplete = true;
  }

  public boolean isComplete() {
    return this.isComplete;
  }

  double maxSpeed() {
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

  Vector3 getProgress(double segmentCompletion) {
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

  public Segment extend(double distance) {
    if (type == Type.ARC) {
      throw new IllegalStateException("Doh!");
    }

    double deltaT = endTime - startTime;
    Vector3 velocity = end.minus(start);
    double speed = velocity.magnitude();
    double averageSpeed = speed / deltaT;

    Segment newSegment;
    if (type == Type.STRAIGHT) {
      newSegment = Segment.straight(this.end, this.end.plus(velocity.toMagnitude(distance)));
    } else {
      newSegment = Segment.jump(this.end, this.end.plus(velocity.toMagnitude(distance)));
    }
    newSegment.startTime = this.endTime;
    newSegment.endTime = this.endTime + (distance / averageSpeed);
    return newSegment;
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

  double flatDistance() {
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

  Pair<Segment, Segment> splitSegment(double distance, double time) {
    Vector3 splitPosition = getPosition(distance);

    Segment head;
    Segment tail;
    switch (type) {
      case STRAIGHT:
        head = Segment.straight(this.start, splitPosition);
        tail = Segment.straight(splitPosition, this.end);
        break;
      case ARC:
        head = Segment.arc(this.start, splitPosition, this.circle, this.clockWise);
        tail = Segment.arc(splitPosition, this.end, this.circle, this.clockWise);
        break;
      case JUMP:
        head = Segment.jump(this.start, splitPosition);
        tail = Segment.jump(splitPosition, this.end);
        break;
      default:
        throw new IllegalStateException("Unsupported type: " + type);
    }

    head.startTime = this.startTime;
    head.endTime = time;
    tail.startTime = time;

    return Pair.of(head, tail);
  }

  // Use GetProgress instead.
  private Vector3 getPosition(double distance) {
    if (type == Type.STRAIGHT || type == Type.JUMP) {
      return end.minus(start).multiply(distance / flatDistance()).plus(start);
    } else if (type == Type.ARC) {
      double partialRadians = getRadians() * distance / flatDistance();
      return Circle.pointOnCircle(circle.center, circle.radius, circle.radianOffset(start) + partialRadians);
    }
    throw new IllegalStateException("Unsupported type " + type);
  }

  private double calculateArcLength() {
    return Math.abs(getRadians()) * circle.radius;
  }
}
