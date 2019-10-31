package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.flat.Physics;
import rlbot.flat.PredictionSlice;

public class Tactic {

  private static final double MIN_DISTANCE = 80;

  // DO NOT CHANGE THIS.
  public Moment target;
  public final Type type;

  public Tactic(PredictionSlice prediction, Type type) {
    Physics physics = prediction.physics();

    this.target = new Moment(physics.location(), physics.velocity(), prediction.gameSeconds());
    this.type = type;
  }

  public Tactic(Vector3 vector3, Type type) {
    this.target = new Moment(vector3);
    this.type = type;
  }

  public enum Type {
    // These may be more maneuvers and not tactics.
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
//      target = input.ball.position;
//    }
  }

  public Vector3 getTarget() {
    return target.position;
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
      return this.type == t.type && this.target.equals(t.target);
    }

    return false;
  }
}
