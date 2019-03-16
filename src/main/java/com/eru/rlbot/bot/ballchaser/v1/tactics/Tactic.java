package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.flat.Physics;
import rlbot.flat.PredictionSlice;

public class Tactic {

  private static final double MIN_DISTANCE = 30;

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

  public boolean isDone(DataPacket input) {
    if (input.car.position.distance(target.position) < MIN_DISTANCE) {
      return true;
    }

    return false;
  }

  public enum Type {
    GRAB_BOOST,
    HIT_BALL,
    DEMO,
    WALL_RIDE,
    DEFEND,

    // These may be more maneuvers and not tactics.
    FRONT_FLIP,
    DOUBLE_JUMP,
    HALF_FLIP,
    STALL,
    FAST_AERIAL;

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
    // TODO: Update to take the shot or pass or w/e
    if (type == Type.HIT_BALL) {

    }

    return target.position;
  }
}
