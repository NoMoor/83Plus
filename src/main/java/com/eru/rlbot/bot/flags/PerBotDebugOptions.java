package com.eru.rlbot.bot.flags;

import java.util.concurrent.ConcurrentHashMap;

public class PerBotDebugOptions {

  private static final ConcurrentHashMap<Integer, PerBotDebugOptions> BOTS = new ConcurrentHashMap<>();

  private final int playerIndex;

  private volatile boolean renderAllDebugLinesEnabled;
  private volatile boolean renderPlan;
  private volatile boolean trailRendererEnabled;
  private volatile boolean renderBallPrediction;

  private volatile boolean freezeCar;
  private volatile boolean predictAndRenderNextCarFrame;

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

  public boolean isTrailRendererEnabled() {
    return trailRendererEnabled || renderAllDebugLinesEnabled;
  }

  public void setTrailRendererEnabled(boolean trailRendererEnabled) {
    this.trailRendererEnabled = trailRendererEnabled;
  }

  public boolean isRenderBallPrediction() {
    return renderBallPrediction || renderAllDebugLinesEnabled;
  }

  public void setRenderBallPrediction(boolean renderBallPrediction) {
    this.renderBallPrediction = renderBallPrediction;
  }

  public boolean isFreezeCar() {
    return freezeCar;
  }

  public void setFreezeCar(boolean freezeCar) {
    this.freezeCar = freezeCar;
  }

  public boolean isPredictAndRenderNextCarFrame() {
    return predictAndRenderNextCarFrame;
  }

  public void setPredictAndRenderNextCarFrame(boolean predictAndRenderNextCarFrame) {
    this.predictAndRenderNextCarFrame = predictAndRenderNextCarFrame;
  }
}
