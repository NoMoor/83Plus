package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.flat.PredictionSlice;

// TODO: Some subclasses of tactic should have a second target (eg. Strike)
public class Tactic {

  private static final double MIN_DISTANCE = 80;

  // DO NOT CHANGE THIS.
  public final Moment targetMoment;
  public final Type type;

  public Tactic(PredictionSlice prediction, Type type) {
    this.targetMoment = new Moment(prediction);
    this.type = type;
  }

  public Tactic(Moment moment, Type type) {
    this.targetMoment = moment;
    this.type = type;
  }

  public Tactic(Vector3 vector3, Type type) {
    this.targetMoment = new Moment(vector3, Vector3.zero());
    this.type = type;
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

  private boolean isMovingTarget() {
    return type == Type.HIT_BALL || type == Type.DEMO;
  }

  public void updateTactic(DataPacket input) {
//    if (type == Type.HIT_BALL) {
//      targetMoment = input.ball.position;
//    }
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
