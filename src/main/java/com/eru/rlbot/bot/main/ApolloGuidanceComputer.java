package com.eru.rlbot.bot.main;

import static com.eru.rlbot.bot.common.Goal.opponentGoal;
import static com.eru.rlbot.bot.common.Goal.ownGoal;

import com.eru.rlbot.bot.CarBallContactManager;
import com.eru.rlbot.bot.chat.RadioModule;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.common.StateSetChecker;
import com.eru.rlbot.bot.common.Teams;
import com.eru.rlbot.bot.flags.PerBotDebugOptions;
import com.eru.rlbot.bot.prediction.BallPredictionUtil;
import com.eru.rlbot.bot.prediction.CarLocationPredictor;
import com.eru.rlbot.bot.prediction.NextFramePredictor;
import com.eru.rlbot.bot.renderer.BallPredictionRenderer;
import com.eru.rlbot.bot.renderer.BotRenderer;
import com.eru.rlbot.bot.renderer.TrailRenderer;
import com.eru.rlbot.bot.strats.Rotations;
import com.eru.rlbot.bot.strats.StrategyManager;
import com.eru.rlbot.bot.utils.ComputeTracker;
import com.eru.rlbot.common.StateLogger;
import com.eru.rlbot.common.boost.BoostManager;
import com.eru.rlbot.common.dropshot.DropshotTileManager;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.Controls;
import com.eru.rlbot.testing.KickoffGame;
import com.eru.rlbot.testing.PowerSlideTestRig;
import com.eru.rlbot.testing.SlowGameNearBall;
import com.eru.rlbot.testing.TrainingId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rlbot.Bot;
import rlbot.ControllerState;
import rlbot.flat.GameTickPacket;

/**
 * The only way to fly.
 */
public final class ApolloGuidanceComputer implements Bot {

  private static final Logger logger = LogManager.getLogger("Agc");

  public final Goal opponentsGoal;
  public final Goal ownGoal;

  protected final int serialNumber;
  public final String name;
  public final int team;

  public final BotRenderer botRenderer;
  protected final RadioModule radioModule;

  private final StrategyManager strategyManager;
  private final BallPredictionRenderer ballPredictionRenderer;

  public ApolloGuidanceComputer(int serialNumber, String name, int team) {
    this.serialNumber = serialNumber;
    this.name = name;
    this.team = team;

    this.botRenderer = BotRenderer.forBot(this);
    this.radioModule = RadioModule.create(this);

    opponentsGoal = opponentGoal(team);
    ownGoal = ownGoal(team);

    strategyManager = new StrategyManager(this);
    ballPredictionRenderer = new BallPredictionRenderer(serialNumber);
  }

  /**
   * This is the most important function. It will automatically get called by the framework with fresh data
   * every frame. Respond with appropriate controls!
   */
  @Override
  public ControllerState processInput(GameTickPacket packet) {
    long startTime = System.nanoTime();

    if (packet.playersLength() <= serialNumber || packet.ball() == null || !packet.gameInfo().isRoundActive()) {
      // Just return immediately if something looks wrong with the data. This helps us avoid stack traces.
      return Controls.create();
    }

    // Update the boost manager and tile manager with the latest data
    BoostManager.track(packet);
    DropshotTileManager.loadGameTickPacket(packet);
    Teams.track(packet);

    // Translate the raw packet data (which is in an unpleasant format) into our custom DataPacket class.
    // The DataPacket might not include everything from GameTickPacket, so improve it if you need to!
    DataPacket input = new DataPacket(packet, serialNumber);

    // Checks to see if the ball has been touched.
    BallPredictionUtil.refresh(input);
    StateSetChecker.track(input);
    JumpManager.trackInput(input);
    CarBallContactManager.track(input);
    StateLogger.track(input);
    TrainingId.track(input);
    CarLocationPredictor.track(input);

    KickoffGame.track(input);
    SlowGameNearBall.track(input);

    ComputeTracker.init(input.serialNumber);
    Rotations rotations = Rotations.get(input);
    rotations.track(input);

    // TODO: Remove this.
    if (false) {
      Controls controls = Controls.create();
      PowerSlideTestRig.execute(input, controls);
      botRenderer.renderInfo(input, controls);
      return controls;
    }

    radioModule.sendMessages(input);

    Controls output = strategyManager.executeStrategy(input);

    if (PerBotDebugOptions.get(input.car.serialNumber).isImmobilizeCar()) {
      output = Controls.create()
          .withThrottle(0f)
          .withSteer(0)
          .withBoost(false);
    }

    ComputeTracker.stop(input.serialNumber);

    // Must do ball before updating the jump manager
    NextFramePredictor.getPrediction(input, output);
    JumpManager.trackOutput(input, output);

    // Do Rendering.
    Rotations.render(input);
    TrailRenderer.render(input, output);
    botRenderer.renderInfo(input, output);
    ballPredictionRenderer.renderBallPrediction();

    long endTime = System.nanoTime();
    double frameTime = (endTime - startTime) / Constants.NANOS;
    if (frameTime > Constants.STEP_SIZE) {
      logger.error("Dropped frames: {}", (int) (frameTime / Constants.STEP_SIZE));
    }

    return output;
  }

  @Override
  public int getIndex() {
    return this.serialNumber;
  }

  public void retire() {
    System.out.println(String.format("Retiring %s bot %d", name, serialNumber));
  }

  public String getName() {
    return name;
  }
}
