package com.eru.rlbot.bot.common;

import com.eru.rlbot.bot.strats.BallPredictionUtil;
import com.eru.rlbot.bot.strats.BallPredictionUtil.ExaminedBallData;
import com.google.flatbuffers.FlatBufferBuilder;
import rlbot.cppinterop.RLBotDll;
import rlbot.render.RenderPacket;
import rlbot.render.Renderer;
import java.awt.*;
import java.util.Optional;

public class BallPredictionRenderer extends Renderer {

  // Non-static members.
  private RenderPacket previousPacket;

  public BallPredictionRenderer(int index) {
    super(1 + index);
  }

  private void initTick() {
    builder = new FlatBufferBuilder(1000);
  }

  private void sendData() {
    RenderPacket packet = doFinishPacket();
    if (!packet.equals(previousPacket)) {
      RLBotDll.sendRenderPacket(packet);
      previousPacket = packet;
    }
  }

  boolean isInitialized() {
    return builder != null;
  }

  public void renderBallPrediction() {
    if (false)
      return;

    if (!isInitialized()) {
      initTick();
    }

    ExaminedBallData prev = null;
    Color color = Color.BLACK;
    for (ExaminedBallData next : BallPredictionUtil.getPredictions()) {
      if (prev == null) {
        prev = next;
      } else if (next.ball.elapsedSeconds - prev.ball.elapsedSeconds > .1) {
        Optional<Boolean> hittable = next.isHittable();
        if (hittable.isPresent()) {
          color = hittable.get() ? Color.GREEN : Color.RED;
        }

        drawLine3d(color,
            next.ball.position,
            prev.ball.position);
        prev = next;
      }
    }

    if (isInitialized()) {
      sendData();
    }
  }
}
