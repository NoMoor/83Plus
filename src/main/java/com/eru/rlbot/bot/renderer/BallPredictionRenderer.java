package com.eru.rlbot.bot.renderer;

import com.eru.rlbot.bot.flags.PerBotDebugOptions;
import com.eru.rlbot.bot.prediction.BallPredictionUtil;
import com.eru.rlbot.bot.prediction.BallPredictionUtil.ExaminedBallData;
import com.google.flatbuffers.FlatBufferBuilder;
import java.awt.Color;
import java.util.Optional;
import rlbot.cppinterop.RLBotDll;
import rlbot.render.RenderPacket;
import rlbot.render.Renderer;

/**
 * Renders the ball prediction line.
 */
public class BallPredictionRenderer extends Renderer {

  // Non-static members.
  private RenderPacket previousPacket;

  private int playerIndex;

  public BallPredictionRenderer(int index) {
    super(50 + index);
    playerIndex = index;
  }

  /**
   * Renders the ball prediction path.
   */
  public void renderBallPrediction() {
    if (!PerBotDebugOptions.get(playerIndex).isRenderBallPrediction()) {
      return;
    }

    if (!isInitialized()) {
      initTick();
    }

    ExaminedBallData prev = null;
    for (ExaminedBallData next : BallPredictionUtil.get(playerIndex).getPredictions()) {
      if (prev == null) {
        prev = next;
      } else if (next.ball.time - prev.ball.time > .1) {
        Optional<Boolean> hittable = next.isHittable();
        Color color = !hittable.isPresent() ? Color.BLACK : hittable.get() ? Color.GREEN : Color.RED;

        drawLine3d(color,
            next.ball.position,
            prev.ball.position);
        prev = next;
      }
    }

    sendData();
  }

  /**
   * Ensures that the flatbuffer is initialized.
   */
  private void initTick() {
    builder = new FlatBufferBuilder(1000);
  }

  /**
   * Sends the render data if it is different from the previous tick.
   */
  private void sendData() {
    RenderPacket packet = doFinishPacket();
    if (!packet.equals(previousPacket)) {
      RLBotDll.sendRenderPacket(packet);
      previousPacket = packet;
    }
  }

  /**
   * Returns true if the render packet is initialized.
   */
  private boolean isInitialized() {
    return builder != null;
  }
}
