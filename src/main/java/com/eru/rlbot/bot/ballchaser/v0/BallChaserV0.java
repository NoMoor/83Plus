package com.eru.rlbot.bot.ballchaser.v0;

import com.eru.rlbot.bot.common.BotChatter;
import com.eru.rlbot.bot.common.BotRenderer;
import com.eru.rlbot.common.boost.BoostManager;
import com.eru.rlbot.common.dropshot.DropshotTileManager;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector2;
import rlbot.Bot;
import rlbot.ControllerState;
import rlbot.flat.GameTickPacket;

public class BallChaserV0 implements Bot {

    private final int playerIndex;
    private final BotRenderer botRenderer;
    private final BotChatter botChatter;

    public BallChaserV0(int playerIndex) {
        this.playerIndex = playerIndex;

        this.botRenderer = BotRenderer.forBot(this);
        this.botChatter = BotChatter.forBot(this);
    }

    /**
     * This is where we keep the actual bot logic. This function shows how to chase the ball.
     * Modify it to make your bot smarter!
     */
    private ControlsOutput processInput(DataPacket input) {

        Vector2 ballPosition = input.ball.position.flatten();
        CarData myCar = input.car;
        Vector2 carPosition = myCar.position.flatten();
        Vector2 carDirection = myCar.orientation.noseVector.flatten();

        // Subtract the two positions to get a vector pointing from the car to the ball.
        Vector2 carToBall = ballPosition.minus(carPosition);

        // How far does the car need to rotate before it's pointing exactly at the ball?
        double steerCorrectionRadians = carDirection.correctionAngle(carToBall);

        return new ControlsOutput()
            .withSteer((float) steerCorrectionRadians * -1)
            .withSlide(Math.abs(steerCorrectionRadians) > 1)
            // If you are pointed at the ball
            .withBoost(steerCorrectionRadians < .1 && input.car.boost > 20)
            // TODO(ahatfield): Update to be something else.
            .withThrottle(1);
    }

    /**
     * This is the most important function. It will automatically get called by the framework with fresh data
     * every frame. Respond with appropriate controls!
     */
    @Override
    public ControllerState processInput(GameTickPacket packet) {
        if (packet.playersLength() <= playerIndex || packet.ball() == null || !packet.gameInfo().isRoundActive()) {
            // Just return immediately if something looks wrong with the data. This helps us avoid stack traces.
            return new ControlsOutput();
        }

        // Update the boost manager and tile manager with the latest data
        BoostManager.loadGameTickPacket(packet);
        DropshotTileManager.loadGameTickPacket(packet);

        // Translate the raw packet data (which is in an unpleasant format) into our custom DataPacket class.
        // The DataPacket might not include everything from GameTickPacket, so improve it if you need to!
        DataPacket dataPacket = new DataPacket(packet, playerIndex);

        botRenderer.draw(dataPacket);
        botChatter.talk(dataPacket);

        return processInput(dataPacket);
    }

    @Override
    public int getIndex() {
        return this.playerIndex;
    }

    public void retire() {
        System.out.println("Retiring BallChaser V0 bot " + playerIndex);
    }
}
