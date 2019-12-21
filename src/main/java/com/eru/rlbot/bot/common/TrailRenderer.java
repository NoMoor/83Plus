package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;
import com.google.flatbuffers.FlatBufferBuilder;
import rlbot.cppinterop.RLBotDll;
import rlbot.render.RenderPacket;
import rlbot.render.Renderer;
import java.awt.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class TrailRenderer {

  // Max legnth of trail to render.
  private static final int MAX_SIZE = (int) (Constants.STEP_SIZE_COUNT * 1.5);
  private static final int GROUPING_SIZE = 10;

  // Groupings of trail packets that are rendered together.
  private static LinkedList<ImmutableList<Pair<DataPacket, ControlsOutput>>> TRAIL_PACKETS = new LinkedList<>();
  private static LinkedList<Pair<DataPacket, ControlsOutput>> NEW_PACKETS = new LinkedList<>();

  private static int nextTrailBotIndex = 100;
  private static Map<ImmutableList<Pair<DataPacket, ControlsOutput>>, TrailRendererInternal> TRAIL_BOTS =
      new HashMap<>();
  private static LinkedList<TrailRendererInternal> RENDER_POOL = new LinkedList<>();

  public static void recordAndRender(DataPacket input, ControlsOutput output) {
    record(input, output);
    renderTrail();
  }

  private static void renderTrail() {
    TrailRendererInternal renderer = getTrailRenderer();
    Pair<DataPacket, ControlsOutput> previous = TRAIL_PACKETS.isEmpty()
        ? null
        : TRAIL_PACKETS.getLast().get(TRAIL_PACKETS.getLast().size() - 1);

    for (Pair<DataPacket, ControlsOutput> trail : NEW_PACKETS) {
      if (previous != null) {
        renderTrail(renderer, previous, trail);
      }

      previous = trail;
    }
    if (renderer.isInitialized()) {
      renderer.sendData();
    }
  }

  private static final float MAX_VEL_HEIGHT = Constants.BALL_RADIUS / 2;

  private static void renderTrail(
      Renderer renderer,
      Pair<DataPacket, ControlsOutput> previous,
      Pair<DataPacket, ControlsOutput> next) {

    Vector3 prevPosition = previous.getFirst().car.position;
    Vector3 nextPosition = next.getFirst().car.position;

    if (prevPosition.distance(nextPosition) > 50) {
      // The car has jumped. Skip rendering this cell.
      return;
    }

    // Connect Segments
    drawSteering(renderer, previous, next);

    // Draw speed / acceleration change
    drawAcceleration(renderer, previous.getFirst(), next.getFirst());
  }

  private static void drawSteering(
      Renderer renderer, Pair<DataPacket, ControlsOutput> previous, Pair<DataPacket, ControlsOutput> next) {

    Vector3 prevPosition = previous.getFirst().car.position;
    Vector3 nextPosition = next.getFirst().car.position;

    Vector3 prevToNext = nextPosition.minus(prevPosition);
    double distance = next.getFirst().car.velocity.norm() / Constants.STEP_SIZE_COUNT;
    if (prevToNext.isZero() || distance == 0) {
      return;
    }

    Vector3 steeringVector = Angles3.rotationMatrix(-previous.getSecond().getSteer())
        .dot(prevToNext)
        .toMagnitude(distance * 2);

    renderer.drawLine3d(getSteerColor(previous), prevPosition, prevPosition.plus(steeringVector));
  }

  private static void drawAcceleration(Renderer renderer, DataPacket previous, DataPacket next) {
    Vector3 prevPosition = previous.car.position;

    // Lean toward the acceleration.
    Vector3 nextVel = next.car.velocity;
    Vector3 prevVel = previous.car.velocity;
    Vector3 speedDiff = nextVel.minus(prevVel);
    Vector3 speedDirection = speedDiff.isZero()
        ? previous.car.orientation.getNoseVector()
        : speedDiff.normalized();

    // Have a length of the total speed.
    double speed = previous.car.velocity.norm();
    double speedVectorHeight = (speed / Constants.BOOSTED_MAX_SPEED) * MAX_VEL_HEIGHT;

    Vector3 speedVector = previous.car.orientation.getRoofVector().plus(speedDirection).normalized()
        .toMagnitude(speedVectorHeight);
    renderer.drawLine3d(getSpeedColor(prevVel, nextVel), prevPosition, prevPosition.plus(speedVector));
  }

  private static Color getSpeedColor(Vector3 prevVel, Vector3 nextVel) {
    double totalDiff = nextVel.norm() - prevVel.norm();
    if (totalDiff == 0) {
      return Color.WHITE;
    } else {
      double relativeAcceleration = totalDiff / (Constants.BOOSTED_ACCELERATION / 120);
      float shift = (float) Angles3.clip(1 - Math.abs(relativeAcceleration), 0, 1);
      return relativeAcceleration > 0
          ? new Color(shift, 1, shift)
          : new Color(1, shift, shift);
    }
  }

  private static Color getSteerColor(Pair<DataPacket, ControlsOutput> previous) {
    float steer = previous.getSecond().getSteer();
    if (steer == 0f) {
      // Straight.
      return new Color(255, 255, 255);
    } else if (steer > 0) {
      // Right turn.
      int shift = (int) (255 * (1 - Math.abs(steer)));
      return new Color(shift, 255, shift);
    } else {
      // Left Turn.
      int shift = (int) (255 * (1 - Math.abs(steer)));
      return new Color(255, shift, shift);
    }
  }

  private static TrailRendererInternal getTrailRenderer() {
    if (RENDER_POOL.isEmpty()) {
      RENDER_POOL.add(new TrailRendererInternal(nextTrailBotIndex++));
    }

    TrailRendererInternal renderer = RENDER_POOL.getFirst();
    if (!renderer.isInitialized()) {
      renderer.initTick();
    }
    return renderer;
  }

  private static void record(DataPacket input, ControlsOutput output) {
    if (NEW_PACKETS.size() > GROUPING_SIZE) {
      ImmutableList<Pair<DataPacket, ControlsOutput>> packets = ImmutableList.copyOf(NEW_PACKETS);
      TRAIL_PACKETS.addLast(packets);
      TRAIL_BOTS.put(packets, RENDER_POOL.removeFirst());

      NEW_PACKETS.clear();
    }

    if (TRAIL_PACKETS.size() * GROUPING_SIZE > MAX_SIZE) {
      ImmutableList<Pair<DataPacket, ControlsOutput>> removedPacket = TRAIL_PACKETS.removeFirst();
      RENDER_POOL.add(TRAIL_BOTS.remove(removedPacket));
    }

    NEW_PACKETS.add(Pair.of(input, output));
  }

  // The offset in render group index to not collide with the cars.
  private static final int TRAIL_RENDERER_OFFSET = 100;

  private static class TrailRendererInternal extends Renderer {
    // Non-static members.
    private RenderPacket previousPacket;

    private TrailRendererInternal(int index) {
      super(index + TRAIL_RENDERER_OFFSET);
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
  }
}
