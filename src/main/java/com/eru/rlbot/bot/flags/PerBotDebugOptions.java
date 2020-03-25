package com.eru.rlbot.bot.flags;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Model holding UI selections for each bot.
 */
public final class PerBotDebugOptions {

  private static final ConcurrentHashMap<Integer, PerBotDebugOptions> MAP = new ConcurrentHashMap<>();

  private final int playerIndex;

  private volatile boolean renderAllDebugLinesEnabled;
  private volatile boolean renderLines;
  private volatile boolean renderDebugText;
  private volatile boolean renderCarTrails;
  private volatile boolean renderBallPrediction;

  private volatile boolean immobilizeCar;
  private volatile boolean prerenderNextFrame;
  private volatile boolean renderCarPredictions;
  private volatile boolean renderRotations;

  public PerBotDebugOptions(int playerIndex) {
    this.playerIndex = playerIndex;
  }

  public static PerBotDebugOptions get(int playerIndex) {
    return MAP.computeIfAbsent(playerIndex, PerBotDebugOptions::new);
  }

  public boolean isRenderAllDebugLines() {
    return renderAllDebugLinesEnabled;
  }

  public void setRenderAllDebugLinesEnabled(boolean renderAllDebugLines) {
    this.renderAllDebugLinesEnabled = renderAllDebugLines;
  }

  public boolean isRenderLines() {
    return renderLines || renderAllDebugLinesEnabled;
  }

  public void setRenderLines(boolean renderLines) {
    this.renderLines = renderLines;
  }

  public boolean isRenderCarTrails() {
    // TODO: Fix the trail renderer.
    return false && (renderCarTrails || renderAllDebugLinesEnabled);
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

  public boolean isRenderCarPredictionsEnabled() {
    return renderCarPredictions;
  }

  public void setRenderCarPredictions(boolean renderCarPredictions) {
    this.renderCarPredictions = renderCarPredictions;
  }

  public boolean isRenderDebugText() {
    return renderDebugText;
  }

  public void setRenderDebugText(boolean renderDebugText) {
    this.renderDebugText = renderDebugText;
  }

  public void setRenderRotationsEnabled(boolean renderRotations) {
    this.renderRotations = renderRotations;
  }

  public boolean isRenderRotationsEnabled() {
    return this.renderRotations || renderAllDebugLinesEnabled;
  }
}
