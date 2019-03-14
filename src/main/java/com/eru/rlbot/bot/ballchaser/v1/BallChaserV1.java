package com.eru.rlbot.bot.ballchaser.v1;

import com.eru.rlbot.bot.ballchaser.v1.strats.StrategyManager;
import com.eru.rlbot.bot.common.BotChatter;
import com.eru.rlbot.bot.common.BotRenderer;
import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.common.boost.BoostManager;
import com.eru.rlbot.common.boost.SpeedManager;
import com.eru.rlbot.common.dropshot.DropshotTileManager;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.ControlsOutput;
import rlbot.Bot;
import rlbot.ControllerState;
import rlbot.flat.GameTickPacket;

import static com.eru.rlbot.bot.common.Goal.opponentGoal;
import static com.eru.rlbot.bot.common.Goal.ownGoal;

public class BallChaserV1 implements Bot {

    private final int playerIndex;
    private final Goal opponentsGoal;
    private final Goal ownGoal;

    private final StrategyManager stategyManager;

    // TODO(ahatfield): How do I translate this into a goal that I want to score in?
    private int team;

    private final BotRenderer botRenderer;
    private final BotChatter botChatter;

    public BallChaserV1(int playerIndex, int team) {
        this.playerIndex = playerIndex;
        this.team = team;

        this.botRenderer = BotRenderer.forBot(this);
        this.botChatter = BotChatter.forBot(this);

        stategyManager = new StrategyManager(this);

        opponentsGoal = opponentGoal(team);
        ownGoal = ownGoal(team);
    }

    /**
     * This is where we keep the actual bot logic. This function shows how to chase the ball.
     * Modify it to make your bot smarter!
     */
    private ControlsOutput processInput(DataPacket input) {

        SpeedManager.trackSuperSonic(input);
        stategyManager.updateStrategy(input);

        botRenderer.renderAcceleration(input.car);

        ControlsOutput output = stategyManager.executeStrategy(input);

        botRenderer.renderOutput(output);

        return output;
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
        DataPacket input = new DataPacket(packet, playerIndex);

        JumpManager.loadDataPacket(input);

        botRenderer.draw(input);
        botChatter.talk(input);

        ControlsOutput output = processInput(input);

        JumpManager.processOutput(output, input);

        return output;
    }

    @Override
    public int getIndex() {
        return this.playerIndex;
    }

    public void retire() {
        System.out.println("Retiring BallChaser V1 bot " + playerIndex);
    }
}
