package com.eru.rlbot.bot.common;

import com.eru.rlbot.bot.ballchaser.v1.strats.Strategist;
import com.eru.rlbot.bot.ballchaser.v1.tactics.Tactic;
import com.eru.rlbot.bot.ballchaser.v1.tactics.Tactician;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector2;
import rlbot.Bot;
import rlbot.cppinterop.RLBotDll;
import rlbot.flat.BallPrediction;
import rlbot.manager.BotLoopRenderer;
import rlbot.render.Renderer;
import com.eru.rlbot.common.dropshot.DropshotTile;
import com.eru.rlbot.common.dropshot.DropshotTileManager;
import com.eru.rlbot.common.dropshot.DropshotTileState;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;

import java.awt.*;
import java.io.IOException;
import java.util.*;

/** Renders extra information for the bot such as car path, ball path, etc. */
public class BotRenderer {

  private static final int SMOOTHING_INTERVAL = 5;
  private static Map<Bot, BotRenderer> BOTS = new HashMap<>();

  private Float initialTime = null;
  private long ticks = 0;
  private LinkedList<Vector3> previousVelocities = new LinkedList<>();
  private LinkedList<Float> previousVelocityTimes = new LinkedList<>();

  private static final int TEXT_LIST_START_Y = 120;
  private static final int TEXT_LIST_SPACING_Y = 26;

  private final boolean skipRendering;
  private final Bot bot;

  private BotRenderer(Bot bot) {
    this.bot = bot;
    this.skipRendering = bot.getIndex() != 0;
  }

  public static BotRenderer forBot(Bot bot) {
    BOTS.putIfAbsent(bot, new BotRenderer(bot));
    return BOTS.get(bot);
  }

  public void renderProjection(CarData carData, Vector2 projectedVector) {
    if (skipRendering) {
      return;
    }

    Renderer renderer = getRenderer();

    // Draw a line from the car to the ball
    renderer.drawLine3d(Color.CYAN, carData.position, Vector3.of(projectedVector.x, projectedVector.y, 0));
  }

  private void renderBallPrediction(DataPacket input) {
    Renderer renderer = getRenderer();

    try {
      final BallPrediction ballPrediction = RLBotDll.getBallPrediction();

      Vector3 previousSpot = input.ball.position;
      for (int i = 0 ; i < ballPrediction.slicesLength(); i++) {
        Vector3 location = Vector3.of(ballPrediction.slices(i).physics().location());

        renderer.drawLine3d(Color.CYAN, previousSpot, location);
        previousSpot = location;
      }
    } catch (IOException e) {}
  }

  private Renderer getRenderer() {
    return BotLoopRenderer.forBotLoop(bot);
  }

  public void renderInfo(DataPacket input, ControlsOutput output) {
    if (skipRendering) return;

    renderControl();
    renderRefreshRate(input);
    renderBallPrediction(input);

    renderAcceleration(input);
    renderOutput(output);
    renderText();
    checkAlert(input);
  }

  private void renderRefreshRate(DataPacket input) {
    if (initialTime == null || input.car.elapsedSeconds - initialTime > 5) {
      initialTime = input.car.elapsedSeconds;
      ticks = 0;
    }
    ticks++;

    double fps = (ticks / (input.car.elapsedSeconds - initialTime));

    renderText(0, 20, 1,"%.2f FPS", fps);
  }

  private void renderText() {
    Renderer renderer = getRenderer();

    for (int i = 0 ; i < renderList.size(); i++) {
      RenderedString string = renderList.get(i);
      renderer.drawString2d(string.text, string.color, new Point(0, TEXT_LIST_START_Y + (TEXT_LIST_SPACING_Y * i)), 2, 2);
    }

    renderList.clear();
  }

  private final LinkedList<RenderedString> renderList = new LinkedList<>();

  public void addDebugText(String text, Object... args) {
    addDebugText(Color.WHITE, text, args);
  }

  public void addDebugText(Color color, String text, Object... args) {
    renderList.addFirst(new RenderedString(String.format(text, args), color));
  }

  private float alertTimeSeconds;
  private String alertText;

  public void addAlertText(String alertText, Object... args) {
    this.alertText = String.format(alertText, args);
  }

  private void checkAlert(DataPacket input) {
    if (alertText != null && alertTimeSeconds == 0) {
      alertTimeSeconds = input.car.elapsedSeconds;
    } else if (input.car.elapsedSeconds - alertTimeSeconds > 4) {
      alertTimeSeconds = 0;
      alertText = null;
    }

    if (alertText != null) {
      renderText(Color.RED, 800, 400, 3, alertText);
    }
  }

  private static class RenderedString {
    final String text;
    final Color color;

    RenderedString(String text, Color color) {
      this.text = text;
      this.color = color;
    }
  }

  private Strategist strategist;
  private Tactic tactic;
  private Tactician tactician;
  private String branch;

  private void renderControl() {
    renderText(0, 300,"%s", strategist == null ? "NONE" : strategist.getType());
    renderText(250, 300,"%s", tactic == null ? "NONE" : tactic.type);
    renderText(500, 300,"%s",
        tactician == null ? "NONE" : tactician.getClass().getSimpleName().replace("Tactician", ""));
    renderText(700, 300,"%s", branch);
  }

  public void setStrategy(Strategist strategist) {
    this.strategist = strategist;
  }

  public void setTactic(Tactic tactic) {
    this.tactic = tactic;
  }

  public void setTactician(Tactician tactician) {
    this.tactician = tactician;
  }

  public void setBranchInfo(String branch, Object... args) {
    this.branch = String.format(branch, args);
  }

  private static String lor(double value) {
    return value > 0 ? "RIGHT" : value == 0 ? "NONE" : "LEFT";
  }

  private String ud(float value) {
    return value > 0 ? "UP" : value == 0 ? "NONE" : "DOWN";
  }

  private void renderOutput(ControlsOutput output) {
    renderText(250, 340, String.format("Throttle %.2f", output.getThrottle()));
    renderText(250, 370, String.format("Turn %.2f %s" , output.getSteer(), lor(output.getSteer())));
    renderText(250, 400, String.format("Boost %s" , output.holdBoost()));
    renderText(250, 430, String.format("Drift %s" , output.holdHandbrake()));
    renderText(250, 460, String.format("Jump %s" , output.holdJump()));
    renderText(250, 490, String.format("Pitch %.2f %s" , output.getPitch(), ud(output.getPitch())));
    renderText(250, 520, String.format("Yaw %.2f %s" , output.getYaw(), lor(output.getYaw())));
    renderText(250, 550, String.format("Roll %.2f %s" , output.getRoll(), lor(output.getRoll())));
  }

  private void renderAcceleration(DataPacket input) {
    CarData carData = input.car;

    if (previousVelocities.size() == SMOOTHING_INTERVAL) {

      double deltaV = previousVelocities.peekLast().minus(previousVelocities.peekFirst()).flatten().magnitude();
      double deltaT = previousVelocityTimes.peekLast() - previousVelocityTimes.peekFirst();

      int speed = (int) carData.velocity.flatten().magnitude();
      // Delta V / Delta T
      int acceleration = (int) (deltaV / deltaT);

      renderText(0, 340, String.format("Vel: %d", speed));
      renderText(0, 370, String.format("Acc: %d", acceleration));

      previousVelocities.removeFirst();
      previousVelocityTimes.removeFirst();
    }

    previousVelocities.add(carData.velocity);
    previousVelocityTimes.add(carData.elapsedSeconds);

    BallData relativeBallData = NormalUtils.noseNormal(input);

    renderText(0, 400, "Z: %d", (int) relativeBallData.position.z);
    renderText(0, 430, "Y: %d", (int) relativeBallData.position.y);
    renderText(0, 460, "X: %d", (int) relativeBallData.position.x);

    renderText(0, 490, "dZ: %d", (int) relativeBallData.velocity.z);
    renderText(0, 520, "dY: %d", (int) relativeBallData.velocity.y);
    renderText(0, 550, "dX: %d", (int) relativeBallData.velocity.x);
  }

  // Methods for rendering text on screen.
  private void renderText(Color color, int x, int y, int size, String text, Object... args) {
    Renderer renderer = getRenderer();
    renderer.drawString2d(String.format(text, args), color, new Point(x, y), size, size);
  }

  private void renderText(int x, int y, int size, String text, Object... args) {
    renderText(Color.WHITE, x, y, size, text, args);
  }

  private void renderText(Color color, int x, int y, String text, Object... args) {
    renderText(Color.WHITE, x, y, 2, text, args);
  }

  private void renderText(int x, int y, String text, Object... args) {
    renderText(Color.WHITE, x, y, text, args);
  }

  // Unused.
  private void renderDropShot(DataPacket input) {
    Renderer renderer = getRenderer();

    for (DropshotTile tile: DropshotTileManager.getTiles()) {
      if (tile.getState() == DropshotTileState.DAMAGED) {
        renderer.drawCenteredRectangle3d(Color.YELLOW, tile.getLocation(), 4, 4, true);
      } else if (tile.getState() == DropshotTileState.DESTROYED) {
        renderer.drawCenteredRectangle3d(Color.RED, tile.getLocation(), 4, 4, true);
      }
    }

    // Draw a rectangle on the tile that the car is on
    DropshotTile tile = DropshotTileManager.pointToTile(input.car.position.flatten());
    if (tile != null) {
      renderer.drawCenteredRectangle3d(Color.green, tile.getLocation(), 8, 8, false);
    }
  }

  public void render3DLine(Color color, Vector3 loc1, Vector3 loc2) {
    if (!skipRendering)
      getRenderer().drawLine3d(color, loc1, loc2);
  }
}
