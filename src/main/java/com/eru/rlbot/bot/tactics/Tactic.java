package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;
import rlbot.flat.PredictionSlice;
import java.util.function.Function;

/**
 * Definition of the immediate unit of work to do. Specifies the nature of the work to allow tacticians latitude to
 * accomplish the goal however they want to.
 */
public class Tactic {

  public final TacticType tacticType;

  public final Moment subject;
  public final SubjectType subjectType;
  public final Vector3 object;

  public Tactic(Builder builder) {
    subject = builder.moment;
    tacticType = builder.tacticType;
    object = builder.object == null ? builder.moment.position : builder.object;
    subjectType = builder.subjectType;
  }

  public Tactic withType(TacticType newTacticType) {
    return this.toBuilder()
        .setTacticType(newTacticType)
        .build();
  }

  /** Different types of tactics. */
  public enum TacticType {
    AERIAL,
    CATCH,
    DEMO,
    DEFEND,
    DOUBLE_JUMP,
    DRIBBLE,
    FAST_AERIAL,
    FLICK,
    FLIP,
    GRAB_BOOST,
    HALF_FLIP,
    HIT_BALL,
    JUMP_FLIP,
    KICKOFF,
    PICK_UP,
    ROTATE,
    SHADOW,
    STRIKE,
    STALL,
    WALL_RIDE,
    WAVE_DASH
  }

  public Vector3 getTargetPosition() {
    return subject.position;
  }

  @Override
  public String toString() {
    return tacticType.name();
  }

  @Override
  public int hashCode() {
    return tacticType.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (o instanceof Tactic) {
      Tactic t = (Tactic) o;
      return this.tacticType == t.tacticType && this.subject.equals(t.subject);
    }

    return false;
  }

  private Builder toBuilder() {
    return new Builder()
        .setSubjectType(this.subjectType)
        .setSubject(this.subject)
        .setTacticType(this.tacticType)
        .setObject(this.object);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static Builder ballTactic() {
    return new Builder()
        .setSubjectType(SubjectType.BALL);
  }

  public static Builder wayPointTactic() {
    return new Builder()
        .setSubjectType(SubjectType.WAY_POINT);
  }

  public static Builder waySmallBoostTactic() {
    return new Builder()
        .setSubjectType(SubjectType.SMALL_BOOST);
  }

  public static Builder wayLargeBoostTactic() {
    return new Builder()
        .setSubjectType(SubjectType.LARGE_BOOST);
  }

  /** A builder pattern for creating tactics. */
  public static class Builder {

    private TacticType tacticType;
    private Moment moment;
    private SubjectType subjectType;

    private Vector3 object;

    public Builder setTacticType(TacticType tacticType) {
      this.tacticType = tacticType;
      return this;
    }

    public Builder setSubject(PredictionSlice predictionSlice) {
      this.moment = new Moment(predictionSlice);
      return this;
    }

    public Builder setSubject(Moment moment) {
      this.moment = moment;
      return this;
    }

    public Builder setSubject(Vector3 location) {
      this.moment = new Moment(location, Vector3.zero());
      return this;
    }

    public Builder setSubjectType(SubjectType subjectType) {
      this.subjectType = subjectType;
      return this;
    }

    public Builder setObject(Vector3 object) {
      this.object = object;
      return this;
    }

    public Tactic build() {
      return new Tactic(this);
    }

    public ImmutableList<Tactic> plan(Function<Tactic, ImmutableList<Tactic>> planner) {
      return planner.apply(new Tactic(this));
    }
  }

  public enum SubjectType {
    BALL,
    CAR,
    WAY_POINT,
    SMALL_BOOST,
    LARGE_BOOST;

    boolean isBoost() {
      return this == SMALL_BOOST || this == LARGE_BOOST;
    }

    boolean isBall() {
      return this == BALL;
    }
  }
}
