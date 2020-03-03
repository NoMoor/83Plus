package com.eru.rlbot.common.output;

import rlbot.ControllerState;

/**
 * Controls to send.
 */
public final class Controls implements ControllerState {

  // 0 is straight, -1 is hard left, 1 is hard right.
  private float steer;

  // -1 for front flip, 1 for back flip
  private float pitch;

  // 0 is straight, -1 is hard left, 1 is hard right.
  private float yaw;

  // 0 is straight, -1 is hard left, 1 is hard right.
  private float roll;

  // 0 is none, -1 is backwards, 1 is forwards
  private float throttle;

  private boolean jumpDepressed;
  private boolean boostDepressed;
  private boolean slideDepressed;
  private boolean useItemDepressed;

  public static Controls create() {
    return new Controls();
  }

  private Controls() {
  }

  public Controls withSteer(float steer) {
    this.steer = clamp(steer);
    return this;
  }

  public Controls withSteer(double steer) {
    return withSteer((float) steer);
  }

  public Controls withPitch(float pitch) {
    this.pitch = clamp(pitch);
    return this;
  }

  public Controls withPitch(double pitch) {
    return withPitch((float) pitch);
  }

  public Controls withYaw(float yaw) {
    this.yaw = clamp(yaw);
    return this;
  }

  public Controls withYaw(double yaw) {
    return withYaw((float) yaw);
  }

  public Controls withRoll(float roll) {
    this.roll = clamp(roll);
    return this;
  }

  public Controls withThrottle(float throttle) {
    this.throttle = clamp(throttle);
    return this;
  }

  public Controls withThrottle(double throttle) {
    return withThrottle((float) throttle);
  }

  public Controls withJump(boolean jumpDepressed) {
    this.jumpDepressed = jumpDepressed;
    return this;
  }

  public Controls withBoost(boolean boostDepressed) {
    this.boostDepressed = boostDepressed;
    return this;
  }

  public Controls withSlide(boolean slideDepressed) {
    this.slideDepressed = slideDepressed;
    return this;
  }

  public Controls withUseItem(boolean useItemDepressed) {
    this.useItemDepressed = useItemDepressed;
    return this;
  }

  public Controls withJump() {
    this.jumpDepressed = true;
    return this;
  }

  public Controls withBoost() {
    this.boostDepressed = true;
    return this;
  }

  public Controls withSlide() {
    this.slideDepressed = true;
    return this;
  }

  public Controls withUseItem() {
    this.useItemDepressed = true;
    return this;
  }

  public static float clamp(float value) {
    return Math.max(-1, Math.min(1, value));
  }

  public static double clamp(double value) {
    return Math.max(-1, Math.min(1, value));
  }

  @Override
  public float getSteer() {
    return steer;
  }

  @Override
  public float getThrottle() {
    return throttle;
  }

  @Override
  public float getPitch() {
    return pitch;
  }

  @Override
  public float getYaw() {
    return yaw;
  }

  @Override
  public float getRoll() {
    return roll;
  }

  @Override
  public boolean holdJump() {
    return jumpDepressed;
  }

  @Override
  public boolean holdBoost() {
    return boostDepressed;
  }

  @Override
  public boolean holdHandbrake() {
    return slideDepressed;
  }

  @Override
  public boolean holdUseItem() {
    return useItemDepressed;
  }
}
