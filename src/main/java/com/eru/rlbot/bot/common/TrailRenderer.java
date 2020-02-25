package com.eru.rlbot.bot.common;

import com.eru.rlbot.bot.flags.Flags;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;
import com.google.flatbuffers.FlatBufferBuilder;
import java.awt.Color;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import rlbot.cppinterop.RLBotDll;
import rlbot.render.RenderPacket;
import rlbot.render.Renderer;

/**
 * Renders the trail of the car including speed and direction changes for easier debugging.
 */
public class TrailRenderer {

  // Max length of trail to render.
  private static final int MAX_SIZE = Constants.STEP_SIZE_COUNT;

  // How to group render calls to ensure we don't overflow the flat buffer size.
  private static final int GROUPING_SIZE = 40;

  // The height to draw the speed component of the trail when the car is traveling at max velocity.
  private static final float MAX_VEL_HEIGHT = Constants.BALL_RADIUS / 2;

  // Counter used to assign trail renderers ids to send the data to the game.
  private static int nextTrailBotIndex = 100;

  // Groupings of trail packets that are rendered together.
  private LinkedList<ImmutableList<Pair<DataPacket, ControlsOutput>>> trailPackets = new LinkedList<>();
  private LinkedList<Pair<DataPacket, ControlsOutput>> newPackets = new LinkedList<>();

  // Maps a list of input/output elements to the renderer that sent them to the game.
  private static Map<ImmutableList<Pair<DataPacket, ControlsOutput>>, TrailRendererInternal> packetRendererMap =
      new HashMap<>();

  // A static pool of trail renderers that can be reused.
  private static LinkedList<TrailRendererInternal> RENDER_POOL = new LinkedList<>();

  // A mapping from a given bot to the trail renderer which captures its data.
  private static final HashMap<Integer, TrailRenderer> RENDERERS = new HashMap<>();

  /** Prevents direct instantiation. Use {@link #render(DataPacket, ControlsOutput)} instead. */
  private TrailRenderer() {
  }

  /** Renders the trail of the car. */
  public static void render(DataPacket input, ControlsOutput output) {
    if (!Flags.ENABLE_TRAIL_RENDERING || !Flags.BOT_RENDERING_IDS.contains(input.car.playerIndex)) {
      return;
    }

    TrailRenderer renderer = getRenderer(input.car.playerIndex);

    renderer.record(input, output);
    renderer.renderTrail();
  }

  private static TrailRenderer getRenderer(int playerIndex) {
    return RENDERERS.computeIfAbsent(playerIndex, i -> new TrailRenderer());
  }

  private void renderTrail() {
    TrailRendererInternal renderer = getTrailRenderer();
    Pair<DataPacket, ControlsOutput> previous = trailPackets.isEmpty()
        ? null
        : trailPackets.getLast().get(trailPackets.getLast().size() - 1);

    for (Pair<DataPacket, ControlsOutput> trail : newPackets) {
      if (previous != null) {
        renderTrail(renderer, previous, trail);
      }

      previous = trail;
    }
    if (renderer.isInitialized()) {
      renderer.sendData();
    }
  }

  private static void renderTrail(
      Renderer renderer, Pair<DataPacket, ControlsOutput> previous, Pair<DataPacket, ControlsOutput> next) {

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

  /** Renders vertical steering trails representing the control outputs at a given point in time. */
  private static void drawSteering(
      Renderer renderer, Pair<DataPacket, ControlsOutput> previous, Pair<DataPacket, ControlsOutput> next) {

    Vector3 prevPosition = previous.getFirst().car.position;
    Vector3 nextPosition = next.getFirst().car.position;

    Vector3 prevToNext = nextPosition.minus(prevPosition);
    double distance = next.getFirst().car.velocity.magnitude() / Constants.STEP_SIZE_COUNT;
    if (prevToNext.isZero() || distance == 0) {
      return;
    }

    Vector3 steeringVector = Angles3.rotationMatrix(-previous.getSecond().getSteer())
        .dot(prevToNext)
        .toMagnitude(distance * 2);

    renderer.drawLine3d(getSteerColor(previous), prevPosition, prevPosition.plus(steeringVector));
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

  /**
   * Renders speed changes.
   */
  private static void drawAcceleration(Renderer renderer, DataPacket previous, DataPacket next) {
    Vector3 prevPosition = previous.car.position;

    // Lean toward the acceleration.
    Vector3 nextVel = next.car.velocity;
    Vector3 prevVel = previous.car.velocity;
    Vector3 speedDiff = nextVel.minus(prevVel);
    Vector3 speedDirection = speedDiff.isZero()
        ? previous.car.orientation.getNoseVector()
        : speedDiff.uncheckedNormalize();

    // Have a length of the total speed.
    double speed = previous.car.velocity.magnitude();
    double speedVectorHeight = (speed / Constants.BOOSTED_MAX_SPEED) * MAX_VEL_HEIGHT;

    Vector3 speedVector = previous.car.orientation.getRoofVector().plus(speedDirection).uncheckedNormalize()
        .toMagnitude(speedVectorHeight);
    renderer.drawLine3d(getSpeedColor(prevVel, nextVel), prevPosition, prevPosition.plus(speedVector));
  }

  private static Color getSpeedColor(Vector3 prevVel, Vector3 nextVel) {
    double totalDiff = nextVel.magnitude() - prevVel.magnitude();
    if (totalDiff == 0) {
      return Color.WHITE;
    } else {
      double relativeAcceleration = totalDiff / (Constants.BOOSTED_ACCELERATION / 120);
      if (relativeAcceleration < Constants.BREAKING_DECELERATION / 120) {
        relativeAcceleration = 0.1;
      }
      float shift = (float) Angles3.clip(1 - Math.abs(relativeAcceleration), 0, 1);
      return relativeAcceleration > 0
          ? new Color(shift, 1, shift)
          : new Color(1, shift, shift);
    }
  }

  /** Returns a trail renderer from the pool of renderers. */
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

  /** Adds the input/output data to the recorded list of entries. These are later used to render the trail. */
  private void record(DataPacket input, ControlsOutput output) {
    if (newPackets.size() > GROUPING_SIZE) {
      ImmutableList<Pair<DataPacket, ControlsOutput>> packets = ImmutableList.copyOf(newPackets);
      trailPackets.addLast(packets);
      packetRendererMap.put(packets, RENDER_POOL.removeFirst());

      newPackets.clear();
    }

    if (trailPackets.size() * GROUPING_SIZE > MAX_SIZE) {
      ImmutableList<Pair<DataPacket, ControlsOutput>> removedPacket = trailPackets.removeFirst();
      RENDER_POOL.add(packetRendererMap.remove(removedPacket));
    }

    newPackets.add(Pair.of(input, output));
  }

  /**
   * The actual renderer used to send data to the game.
   */
  private static class TrailRendererInternal extends Renderer {

    // The offset in render group index to not collide with the cars.
    private static final int TRAIL_RENDERER_OFFSET = 100;

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
