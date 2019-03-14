package com.eru.rlbot.bot.common;

import com.eru.rlbot.bot.ballchaser.v1.tactics.TacticManager;
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

/** Renders extra information for the bot such as car path, ball path, etc. */
public class BotRenderer {

    private Float initalTime = null;
    private long ticks = 0;

    private Bot bot;

    private BotRenderer(Bot bot) {
        this.bot = bot;
    }

    public static BotRenderer forBot(Bot bot) {
        return new BotRenderer(bot);
    }

    public void draw(DataPacket input) {
        renderRate(input);
        renderBallPrediction(input);
        renderDropShot(input);
    }

    private void renderRate(DataPacket input) {
        Renderer renderer = getRenderer();

        if (initalTime == null) {
            initalTime = input.car.elapsedSeconds;
        }
        ticks++;
        renderer.drawString2d(
            String.format("FPS %f", (ticks / (input.car.elapsedSeconds - initalTime))),
            Color.PINK,
            new java.awt.Point(0, 125),
            2,
            2);
    }

    private Vector3 previousVelocity;
    private float previousTime;

    public void renderAcceleration(CarData carData) {
        Renderer renderer = getRenderer();

        if (previousVelocity != null) {
            // Delta V / Delta T
            double acceleration = carData.velocity.minus(previousVelocity).magnitude()
                                      / (carData.elapsedSeconds - previousTime);

            renderer.drawString2d(
                String.format("Acc: %d", (int) acceleration),
                Color.green,
                new java.awt.Point(0, 85),
                2,
                2);
        }
        previousVelocity = carData.velocity;
        previousTime = carData.elapsedSeconds;
    }

    public void renderProjection(CarData carData, Vector2 projectedVector) {
        Renderer renderer = getRenderer();

        // Draw a line from the car to the ball
        renderer.drawLine3d(Color.CYAN, carData.position, new Vector3(projectedVector.x, projectedVector.y, 0));
    }

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
            // TODO(ahatfield): Remove this dumb IOException...
        } catch (IOException e) {
            // Ignore
        }
    }

    private Renderer getRenderer() {
        return BotLoopRenderer.forBotLoop(bot);
    }

    public void renderOutput(ControlsOutput output) {
        Renderer renderer = getRenderer();

        renderer.drawString2d(String.format("Throttle %f", output.getThrottle()), Color.MAGENTA, new java.awt.Point(0, 150), 2, 2);
    }
}
