package com.eru.rlbot.bot.main;

import static com.eru.rlbot.bot.common.Goal.opponentGoal;
import static com.eru.rlbot.bot.common.Goal.ownGoal;

import com.eru.rlbot.bot.CarBallContactManager;
import com.eru.rlbot.bot.common.BallPredictionRenderer;
import com.eru.rlbot.bot.common.BotChatter;
import com.eru.rlbot.bot.common.BotRenderer;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.common.StateSetChecker;
import com.eru.rlbot.bot.common.TrailRenderer;
import com.eru.rlbot.bot.common.TrainingId;
import com.eru.rlbot.bot.flags.PerBotDebugOptions;
import com.eru.rlbot.bot.prediction.CarLocationPredictor;
import com.eru.rlbot.bot.prediction.NextFramePredictor;
import com.eru.rlbot.bot.strats.BallPredictionUtil;
import com.eru.rlbot.bot.strats.StrategyManager;
import com.eru.rlbot.common.StateLogger;
import com.eru.rlbot.common.boost.BoostManager;
import com.eru.rlbot.common.dropshot.DropshotTileManager;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.testing.KickoffGameSetter;
import com.eru.rlbot.testing.SlowGameNearBall;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rlbot.Bot;
import rlbot.ControllerState;
import rlbot.flat.GameTickPacket;

public final class Agc implements Bot {

  private static final Logger logger = LogManager.getLogger("Agc");

  public final Goal opponentsGoal;
  public final Goal ownGoal;

  protected final int playerIndex;
  public final String name;
  public final int team;

  public final BotRenderer botRenderer;
  protected final BotChatter botChatter;

  private final StrategyManager strategyManager;
  private final BallPredictionRenderer ballPredictionRenderer;

  public Agc(int playerIndex, String name, int team) {
    this.playerIndex = playerIndex;
    this.name = name;
    this.team = team;

    this.botRenderer = BotRenderer.forBot(this);
    this.botChatter = BotChatter.forBot(this);

    opponentsGoal = opponentGoal(team);
    ownGoal = ownGoal(team);

    strategyManager = new StrategyManager(this);
    ballPredictionRenderer = new BallPredictionRenderer(playerIndex);
  }

  /**
   * This is the most important function. It will automatically get called by the framework with fresh data
   * every frame. Respond with appropriate controls!
   */
  @Override
  public ControllerState processInput(GameTickPacket packet) {
    long startTime = System.nanoTime();

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

    // Checks to see if the ball has been touched.
    BallPredictionUtil.refresh(input);
    StateSetChecker.track(input);

    JumpManager.trackInput(input);
    CarBallContactManager.track(input);
    StateLogger.track(input);
    TrainingId.track(input);
    CarLocationPredictor.track(input);

    botChatter.talk(input);

    ControlsOutput output = strategyManager.executeStrategy(input);

    if (PerBotDebugOptions.get(input.car.playerIndex).isImmobilizeCar())
      output = new ControlsOutput()
          .withThrottle(0f)
          .withSteer(0)
          .withBoost(false);

    KickoffGameSetter.track(input);
    SlowGameNearBall.track(input);

    // Must do ball before updating the jump manager
    NextFramePredictor.getPrediction(input, output);
    JumpManager.trackOutput(input, output);

    // Do Rendering.
    TrailRenderer.render(input, output);
    botRenderer.renderInfo(input, output);

    ballPredictionRenderer.renderBallPrediction();

    long endTime = System.nanoTime();
    double frameTime = (endTime - startTime) / Constants.NANOS;
    if (frameTime > Constants.STEP_SIZE) {
      logger.error("Dropped frames: {}", (frameTime / Constants.STEP_SIZE));
    }

    return output;
  }

  @Override
  public int getIndex() {
    return this.playerIndex;
  }

  public void retire() {
    System.out.println(String.format("Retiring %s bot %d", name, playerIndex));
  }

  public String getName() {
    return name;
  }
}
