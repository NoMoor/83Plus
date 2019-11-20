package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.flat.PredictionSlice;

// TODO: Some subclasses of tactic should have a second target (eg. Strike)
public class Tactic {

  // DO NOT CHANGE THIS.
  public final Moment targetMoment;
  public final Vector3 targetTarget;
  public final Type type;

  public Tactic(Moment moment, Vector3 targetTarget, Type type) {
    this.targetMoment = moment;
    this.targetTarget = targetTarget;
    this.type = type;
  }

  public Tactic(PredictionSlice prediction, Type type) {
    this(new Moment(prediction), type);
  }

  public Tactic(Moment moment, Type type) {
    this(moment, moment.position, type);
  }

  public Tactic(Vector3 vector3, Type type) {
    this(new Moment(vector3, Vector3.zero()), type);
  }

  public enum Type {
    AERIAL,
    CATCH,
    DEMO,
    DEFEND,
    DOUBLE_JUMP,
    DRIBBLE,
    FAST_AERIAL,
    FLICK,
    FRONT_FLIP,
    GRAB_BOOST,
    HALF_FLIP,
    HIT_BALL,
    KICKOFF,
    PICK_UP,
    ROTATE,
    SHADOW,
    STRIKE,
    STALL,
    WALL_RIDE,
    WAVE_DASH;
  }

  public Vector3 getTargetPosition() {
    return targetMoment.position;
  }

  @Override
  public String toString() {
    return type.name();
  }

  @Override
  public int hashCode() {
    return type.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (o instanceof Tactic) {
      Tactic t = (Tactic) o;
      return this.type == t.type && this.targetMoment.equals(t.targetMoment);
    }

    return false;
  }
}
