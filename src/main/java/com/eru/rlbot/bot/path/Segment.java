package com.eru.rlbot.bot.path;

import com.eru.rlbot.bot.common.Circle;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.common.Pair;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;
import java.util.Objects;

/**
 * A section of a path.
 */
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

  public static Segment jump(Vector3 start, Vector3 end) {
    return new Segment(start, end, Type.JUMP);
  }

  public static Segment flip(Vector3 start, Vector3 end) {
    return new Segment(start, end, Type.FLIP);
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

  private Segment setType(Type type) {
    if (this.type == Type.ARC) {
      throw new IllegalStateException("Cannot convert arc to other type");
    }
    return new Segment(start, end, type);
  }

  double maxSpeed() {
    switch (type) {
      case STRAIGHT:
      case JUMP:
      case FLIP:
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
      case FLIP:
        return end.minus(start).multiply(segmentCompletion).plus(start);
      default:
        throw new IllegalStateException("Doh!");
    }
  }

  public Segment extend(double time, double speed) {
    if (type == Type.ARC) {
      throw new IllegalStateException("Doh!");
    }

    Vector3 direction = end.minus(start);
    Segment newSegment = Segment.straight(this.end, this.end.plus(direction.toMagnitude(time * speed)));

    newSegment.startTime = this.endTime;
    newSegment.endTime = this.endTime + time;
    return newSegment;
  }

  public Double avgSpeed() {
    return flatDistance() / (endTime - startTime);
  }

  public boolean isOnGround() {
    return type == Type.STRAIGHT || type == Type.ARC;
  }

  public enum Type {
    STRAIGHT,
    ARC,
    JUMP,
    FLIP
  }

  private Double distance;

  double flatDistance() {
    if (distance == null) {
      switch (type) {
        case STRAIGHT:
        case JUMP:
        case FLIP:
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
      case FLIP:
        head = Segment.flip(this.start, splitPosition);
        tail = Segment.flip(splitPosition, this.end);
        break;
      default:
        throw new IllegalStateException("Unsupported type: " + type);
    }

    head.startTime = this.startTime;
    head.endTime = time;
    tail.startTime = time;

    return Pair.of(head, tail);
  }

  public Pair<Segment, Segment> splitSegmentWithFlip(double distance, double time) {
    Pair<Segment, Segment> segments = splitSegment(distance, time);

    return Pair.of(segments.getFirst(), segments.getSecond().setType(Type.FLIP));
  }

  // Use GetProgress instead.
  private Vector3 getPosition(double distance) {
    if (type == Type.STRAIGHT || type == Type.JUMP || type == Type.FLIP) {
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

  public double getRadians() {
    return getRadians(circle, start, end, clockWise);
  }

  public static double getRadians(Circle circle, Vector3 start, Vector3 end, boolean clockWise) {
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

  public static double calculateArcLength(Vector3 start, Vector3 end, Circle circle, CarData input) {
    double radians = getRadians(circle, start, end, circle.isClockwise(input));
    return Math.abs(radians) * circle.radius;
  }

  @Override
  public String toString() {
    return String.format("%s d:%d s:%.2f e:%.2f", type.name(), (int) flatDistance(), startTime, endTime);
  }
}
