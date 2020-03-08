package com.eru.rlbot.bot.renderer;

import com.eru.rlbot.bot.common.Teams;
import com.eru.rlbot.bot.flags.PerBotDebugOptions;
import com.eru.rlbot.bot.prediction.BallPrediction;
import com.eru.rlbot.bot.prediction.BallPredictionUtil;
import com.google.flatbuffers.FlatBufferBuilder;
import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

    BallPrediction prev = null;
    for (BallPrediction next : BallPredictionUtil.get(playerIndex).getPredictions()) {
      if (prev == null) {
        prev = next;
      } else if (next.ball.time - prev.ball.time > .1) {
        Map<Integer, List<Integer>> reachableByTeam = next.ableToReach().stream()
            .collect(Collectors.groupingBy(Teams::getTeamForBot));

        Color color = reachableByTeam.isEmpty()
            ? Color.BLACK // Not reachable
            : reachableByTeam.size() == 2 // Reachable by both teams
            ? Color.RED
            : reachableByTeam.containsKey(0)
            ? Color.BLUE
            : Color.ORANGE;

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
