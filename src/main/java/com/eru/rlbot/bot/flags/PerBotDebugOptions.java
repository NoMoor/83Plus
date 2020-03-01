package com.eru.rlbot.bot.flags;

import java.util.concurrent.ConcurrentHashMap;

public class PerBotDebugOptions {

  private static final ConcurrentHashMap<Integer, PerBotDebugOptions> BOTS = new ConcurrentHashMap<>();

  private final int playerIndex;

  private volatile boolean renderAllDebugLinesEnabled;
  private volatile boolean renderPlan;
  private volatile boolean renderCarTrails;
  private volatile boolean renderBallPrediction;

  private volatile boolean immobilizeCar;
  private volatile boolean prerenderNextFrame;
  private volatile boolean renderStats = true;
  private volatile boolean renderOpponentPaths;

  public PerBotDebugOptions(int playerIndex) {
    this.playerIndex = playerIndex;
  }

  public static PerBotDebugOptions get(int playerIndex) {
    return BOTS.computeIfAbsent(playerIndex, PerBotDebugOptions::new);
  }

  public boolean isRenderAllDebugLines() {
    return renderAllDebugLinesEnabled;
  }

  public void setRenderAllDebugLinesEnabled(boolean renderAllDebugLines) {
    this.renderAllDebugLinesEnabled = renderAllDebugLines;
  }

  public boolean isRenderPlan() {
    return renderPlan || renderAllDebugLinesEnabled;
  }

  public void setRenderPlan(boolean renderPlan) {
    this.renderPlan = renderPlan;
  }

  public boolean isRenderCarTrails() {
    return renderCarTrails || renderAllDebugLinesEnabled;
  }

  public void setRenderCarTrails(boolean renderCarTrails) {
    this.renderCarTrails = renderCarTrails;
  }

  public boolean isRenderBallPrediction() {
    return renderBallPrediction || renderAllDebugLinesEnabled;
  }

  public void setRenderBallPrediction(boolean renderBallPrediction) {
    this.renderBallPrediction = renderBallPrediction;
  }

  public boolean isImmobilizeCar() {
    return immobilizeCar;
  }

  public void setImmobilizeCar(boolean immobilizeCar) {
    this.immobilizeCar = immobilizeCar;
  }

  public boolean isPrerenderNextFrame() {
    return prerenderNextFrame;
  }

  public void setPrerenderNextFrame(boolean prerenderNextFrame) {
    this.prerenderNextFrame = prerenderNextFrame;
  }

  public boolean isRenderStats() {
    return renderStats;
  }

  public void setRenderStats(boolean renderStats) {
    this.renderStats = renderStats;
  }

  public boolean isRenderOpponentPaths() {
    return renderOpponentPaths;
  }

  public void setRenderOpponentPaths(boolean renderOpponentPaths) {
    this.renderOpponentPaths = renderOpponentPaths;
  }
}
