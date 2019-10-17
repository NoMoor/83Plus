package com.eru.rlbot.bot.common;

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

    private static final int SMOOTHING_INTERVAL = 20;
    private static Map<Bot, BotRenderer> BOTS = new HashMap<>();

    private Float initialTime = null;
    private long ticks = 0;
    private LinkedList<Vector3> previousVelocities = new LinkedList<>();
    private LinkedList<Float> previousVelocityTimes = new LinkedList<>();

    private Bot bot;
    private static final int TEXT_LIST_START_Y = 120;
    private static final int TEXT_LIST_SPACING_Y = 26;

    private BotRenderer(Bot bot) {
        this.bot = bot;
    }

    public static BotRenderer forBot(Bot bot) {
        BOTS.putIfAbsent(bot, new BotRenderer(bot));
        return BOTS.get(bot);
    }

    public void renderProjection(CarData carData, Vector2 projectedVector) {
        Renderer renderer = getRenderer();

        // Draw a line from the car to the ball
        renderer.drawLine3d(Color.CYAN, carData.position, new Vector3(projectedVector.x, projectedVector.y, 0));
    }

    private void renderBallPrediction(DataPacket input) {
        Renderer renderer = getRenderer();

        try {
            final BallPrediction ballPrediction = RLBotDll.getBallPrediction();

            Vector3 previousSpot = input.ball.position;
            for (int i = 0 ; i < ballPrediction.slicesLength(); i++) {
                Vector3 location = new Vector3(ballPrediction.slices(i).physics().location());

                renderer.drawLine3d(Color.CYAN, previousSpot, location);
                previousSpot = location;
            }
        } catch (IOException e) {}
    }

    private Renderer getRenderer() {
        return BotLoopRenderer.forBotLoop(bot);
    }

    public void renderInfo(DataPacket input, ControlsOutput output) {
        renderRefreshRate(input);
        renderBallPrediction(input);

        renderAcceleration(input);
        renderOutput(output);
        renderText();
    }

    private void renderRefreshRate(DataPacket input) {
        if (initialTime == null) {
            initialTime = input.car.elapsedSeconds;
        }
        ticks++;

        int fps = (int) (ticks / (input.car.elapsedSeconds - initialTime));

        // TODO: Render this somewhere else.
        addDebugText(String.format("FPS: %d", fps), Color.PINK);
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
    private String debugInfo;

    private static String lor(double value) {
        return value > 0 ? "RIGHT" : "LEFT";
    }

    private void renderOutput(ControlsOutput output) {
        // TODO: Add Steering info
        addDebugText(Color.MAGENTA, String.format("Throttle %f", output.getThrottle()));
    }

    // TODO: Creates branch info to make sure our bot isn't pinging back and forth strategies too often
    public void addBranchInfo() {

    }

    public void addDebugText(String text, Object... args) {
        addDebugText(Color.WHITE, text, args);
    }

    public void addDebugText(Color color, String text, Object... args) {
        renderList.addFirst(new RenderedString(String.format(text, args), color));
        debugInfo = String.format(text, args);
    }

    private void renderAcceleration(DataPacket input) {
        CarData carData = input.car;

        if (previousVelocities.size() == SMOOTHING_INTERVAL) {

            double deltaV = previousVelocities.peekLast().minus(previousVelocities.peekFirst()).flatten().magnitude();
            double deltaT = previousVelocityTimes.peekLast() - previousVelocityTimes.peekFirst();

            int speed = (int) carData.velocity.flatten().magnitude();
            // Delta V / Delta T
            int acceleration = (int) (deltaV / deltaT);

            addDebugText(Color.green, String.format("Speed: %d", speed));
            addDebugText(Color.PINK, String.format("Accel: %d", acceleration));

            previousVelocities.removeFirst();
            previousVelocityTimes.removeFirst();
        }

        previousVelocities.add(carData.velocity);
        previousVelocityTimes.add(carData.elapsedSeconds);

        BallData relativeBallData = CarNormalUtils.noseNormalLocation(input);

        // TODO: Move these to be rendered a different way.
        addDebugText("Z: %d", (int) relativeBallData.position.z);
        addDebugText("Y: %d", (int) relativeBallData.position.y);
        addDebugText("X: %d", (int) relativeBallData.position.x);

        addDebugText("dZ: %d", (int) relativeBallData.velocity.z);
        addDebugText("dY: %d", (int) relativeBallData.velocity.y);
        addDebugText("dX: %d", (int) relativeBallData.velocity.x);
    }

    private class RenderedString {
        final String text;
        final Color color;

        RenderedString(String text, Color color) {
            this.text = text;
            this.color = color;
        }
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

    public void render3DLine(Vector3 loc1, Vector3 loc2, Color color) {
        getRenderer().drawLine3d(color, loc1, loc2);
    }
}
