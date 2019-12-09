package com.eru.rlbot.bot.main;

import com.eru.rlbot.bot.CarBallContactManager;
import com.eru.rlbot.bot.common.BotChatter;
import com.eru.rlbot.bot.common.BotRenderer;
import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.strats.StrategyManager;
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

public final class Acg implements Bot {

  public final Goal opponentsGoal;
  public final Goal ownGoal;
  public final int team;
  public final BotRenderer botRenderer;
  protected final int playerIndex;
  protected final BotChatter botChatter;
  private final StrategyManager strategyManager;

  public Acg(int playerIndex, int team) {
    this.playerIndex = playerIndex;
    this.team = team;

    this.botRenderer = BotRenderer.forBot(this);
    this.botChatter = BotChatter.forBot(this);

    opponentsGoal = opponentGoal(team);
    ownGoal = ownGoal(team);

    strategyManager = new StrategyManager(this);
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
    SpeedManager.trackSuperSonic(input);
    CarBallContactManager.loadDataPacket(input);

    botChatter.talk(input);

    ControlsOutput output = strategyManager.executeStrategy(input);

    botRenderer.renderInfo(input, output);

    JumpManager.processOutput(output, input);

    // Uncomment to force car to stay still
//    if (false)
//     output = new ControlsOutput()
//         .withThrottle(1.0f)
//         .withBoost(input.car.groundSpeed < 2000);

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
