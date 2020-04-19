package com.eru.rlbot.bot.renderer;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.eru.rlbot.bot.common.Circle;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.common.RelativeUtils;
import com.eru.rlbot.bot.common.Teams;
import com.eru.rlbot.bot.flags.GlobalDebugOptions;
import com.eru.rlbot.bot.flags.PerBotDebugOptions;
import com.eru.rlbot.bot.path.Path;
import com.eru.rlbot.bot.path.Paths;
import com.eru.rlbot.bot.path.Segment;
import com.eru.rlbot.bot.prediction.BallPredictionUtil;
import com.eru.rlbot.bot.prediction.BallPredictionUtil.ChallengeData;
import com.eru.rlbot.bot.prediction.BallPredictor;
import com.eru.rlbot.bot.strats.Strategist;
import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.bot.tactics.Tactician;
import com.eru.rlbot.bot.utils.ComputeTracker;
import com.eru.rlbot.common.Pair;
import com.eru.rlbot.common.dropshot.DropshotTile;
import com.eru.rlbot.common.dropshot.DropshotTileManager;
import com.eru.rlbot.common.dropshot.DropshotTileState;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.BoundingBox;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.Controls;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.awt.Color;
import java.awt.Point;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rlbot.Bot;
import rlbot.manager.BotLoopRenderer;
import rlbot.render.Renderer;

/**
 * Renders extra information for the bot such as car path, ball path, etc.
 */
public class BotRenderer {

  private static final Logger logger = LogManager.getLogger("BotRenderer");

  private static final int SMOOTHING_INTERVAL = 5;
  private static final double FULL_CIRCLE = Math.PI * 2;

  private static ConcurrentHashMap<Integer, BotRenderer> INSTANCES = new ConcurrentHashMap<>();
  private static ThreadLocal<BotRenderer> INSTANCE = new ThreadLocal<>();

  private float initialIngameTime = 0;
  private long ingameTicks = 0;
  private float ingameFps = 0;
  private long initialWallClockTime = 0;
  private float wallclockFps = 0;
  private int wallClockTicks = 0;
  private LinkedList<Vector3> previousVelocities = new LinkedList<>();
  private LinkedList<Float> previousVelocityTimes = new LinkedList<>();

  private static final int TEXT_LIST_START_X = 300;
  private static final int TEXT_LIST_START_Y = 300;
  private static final int TEXT_LIST_SPACING_Y = 26;

  private final Bot bot;

  private BotRenderer(Bot bot) {
    this.bot = bot;
  }

  public static BotRenderer forBot(Bot bot) {
    return INSTANCES.computeIfAbsent(bot.getIndex(), index -> new BotRenderer(bot));
  }

  public static BotRenderer forIndex(int botIndex) {
    BotRenderer botRenderer = INSTANCE.get();
    if (botRenderer == null) {
      botRenderer = INSTANCES.get(botIndex);
      INSTANCE.set(botRenderer);
    }
    Preconditions.checkState(botRenderer.bot.getIndex() == botIndex,
        "Illegal renderer access: Trying to access " + botRenderer.bot.getIndex() + " by bot " + botIndex);
    return botRenderer;
  }

  public static BotRenderer forCar(CarData car) {
    return forIndex(car.serialNumber);
  }

  public void renderText(Color color, Vector3 location, String format, Object... args) {
    if (PerBotDebugOptions.get(bot.getIndex()).isRenderDebugText()) {
      getRenderer().drawString3d(String.format(format, args), color, location, 2, 2);
    }
  }

  public void render2dText(Color color, int x, int y, String text) {
    if (PerBotDebugOptions.get(bot.getIndex()).isRenderDebugText() || true) {
      getRenderer().drawString2d(text, color, new Point(x, y), 2, 2);
    }
  }

  private boolean skipLineRendering() {
    return !PerBotDebugOptions.get(bot.getIndex()).isRenderLines();
  }

  private boolean skipTextRendering() {
    return !PerBotDebugOptions.get(bot.getIndex()).isRenderDebugText();
  }

  public void renderStateSetWarning() {
    renderText(Color.ORANGE, 300, 150, 4, "State Setting Enabled");
  }

  private static double VISUAL_OFFSET = 1.2;

  public void renderRotation(CarData car, int rotationNumber, boolean hasPriority) {
    Circle coverage = Circle.forPath(car.position, car.groundSpeed);
    double visualCenter = Orientation.fromFlatVelocity(car).toEuclidianVector().yaw;
    Vector3 leftVision = coverage.pointOnCircle(visualCenter - VISUAL_OFFSET);
    Vector3 rightVision = coverage.pointOnCircle(visualCenter + VISUAL_OFFSET);
    Segment segment = Segment.arc(leftVision, rightVision, coverage, true);

    Color color = hasPriority ? Color.YELLOW : car.team == 0 ? Color.CYAN : Color.PINK;
    getRenderer().drawString3d(String.valueOf(rotationNumber), color, car.position.addZ(100), 2, 2);

    renderArc(color, segment);
    getRenderer().drawLine3d(color, car.position, leftVision);
    getRenderer().drawLine3d(color, car.position, rightVision);
    getRenderer().drawLine3d(color, leftVision, rightVision);
  }

  public void renderRegions(ImmutableList<Circle> regions) {
    if (!PerBotDebugOptions.get(bot.getIndex()).isRenderRotationsEnabled()) {
      return;
    }

    for (Circle region : regions) {
      renderCircle(Color.pink, region);
    }
  }

  public void renderAboveCar(CarData car, String format, Object... args) {
    renderText(Color.GREEN, car.position.addZ(100), String.format(format, args));
  }

  private static class RenderRequest {
    private final float renderTimeEnd;
    private final Vector3 source;
    private final Vector3 target;
    private final Color color;
    private String label;

    public RenderRequest(float renderTimeEnd, Vector3 source, Vector3 target, Color color, String label) {
      this.renderTimeEnd = renderTimeEnd;
      this.source = source;
      this.target = target;
      this.color = color;
      this.label = label;
    }
  }

  public void renderInfo(DataPacket input, Controls output) {
    PerBotDebugOptions renderOptions = PerBotDebugOptions.get(input.serialNumber);
    if (renderOptions.isRenderLines()) {
      renderProjections(input);
      // renderHitBox(input.car); // TODO: Add checkbox for rendering the hitbox.
      // renderTurningRadius(input);

//      renderTacticLines(input.car);
      //    renderPredictionDiff(input);
      //    renderRelativeBallData(input);
      //    renderTouchIndicator(input);
    }

    if (renderOptions.isRenderDebugText()) {
      renderControlDebug();
      renderDebugText();
      renderAlert(input);

      renderControls(output);

//      renderBallVel(input);
      renderCarAccel(input);
//      renderCarLocation(input);
      renderChallengeData(input);
    }

    if (GlobalDebugOptions.isRenderStats()) {
      renderStats(input);
    }
  }

  private void renderChallengeData(DataPacket input) {
    Optional<ChallengeData> challengeDataOptional =
        BallPredictionUtil.get(input.car).getChallengeData();
    if (!challengeDataOptional.isPresent()) {
      return;
    }
    ChallengeData challengeData = challengeDataOptional.get();
    int controllingTeam = challengeData.controllingTeam;
    Color color = controllingTeam == 0 ? Color.BLUE : Color.ORANGE;

    renderTarget(color, challengeData.firstTouch.ball.position);

    float timeToTouch = challengeData.firstTouch.ball.time - input.car.elapsedSeconds;
    float timeToSecond = BallPredictionUtil.PREDICTION_TIME_LIMIT;
    if (challengeData.firstTouchByOtherTeam.isPresent()) {
      timeToSecond = challengeData.firstTouchByOtherTeam.get().ball.time - input.car.elapsedSeconds;
      Color otherTouchColor = Teams.otherTeam(challengeData.controllingTeam) == 0 ? Color.BLUE : Color.ORANGE;
      renderTarget(otherTouchColor, challengeData.firstTouchByOtherTeam.get().ball.position);
    }

    renderText(color, 500, 100, "Impact: %.2fs - Pressure: %.2fs", timeToTouch, timeToSecond - timeToTouch);
  }

  private PriorityQueue<RenderRequest> renderRequests =
      new PriorityQueue<>(Comparator.comparingDouble(rr -> rr.renderTimeEnd));

  // TODO: Refactor this to put color first.
  public void renderProjection(Vector3 source, Vector3 target, Color color) {
    renderProjection(source, target, color, 0);
  }

  public void renderProjection(Vector3 source, Vector3 target, Color color, float renderUntil) {
    renderProjection(source, target, color, renderUntil, "");
  }

  public void renderProjection(Vector3 source, Vector3 target, Color color, float renderUntil, String label) {
    renderRequests.add(new RenderRequest(renderUntil, source, target, color, label));
  }

  private void renderProjections(DataPacket input) {
    Renderer renderer = getRenderer();
    renderRequests.forEach(request -> {
      renderer.drawLine3d(request.color, request.source, request.target);
      if (!request.label.isEmpty()) {
        renderer.drawString3d(request.label, request.color, request.target, 2, 2);
      }
    });

    // Remove expired render requests.
    while (!renderRequests.isEmpty() && renderRequests.peek().renderTimeEnd < input.car.elapsedSeconds) {
      renderRequests.poll();
    }
  }

  public void renderTarget(Vector3 target) {
    renderTarget(Color.white, target);
  }

  public void renderTarget(Color color, Vector2 target) {
    renderTarget(color, target.asVector3());
  }

  public void renderTarget(Color color, Vector3 target) {
    if (skipLineRendering()) {
      return;
    }

    int size = 50;
    getRenderer().drawLine3d(color, target.addX(-size).addY(-size).addZ(-size), target.addX(size).addY(size).addZ(size));
    getRenderer().drawLine3d(color, target.addX(-size).addY(size).addZ(-size), target.addX(size).addY(-size).addZ(size));
    getRenderer().drawLine3d(color, target.addX(-size).addY(-size).addZ(size), target.addX(size).addY(size).addZ(-size));
    getRenderer().drawLine3d(color, target.addX(-size).addY(size).addZ(size), target.addX(size).addY(-size).addZ(-size));
  }

  /**
   * Used for rendering multi-color paths. Callers must verify their rendering has been enabled.
   */
  public void renderPaths(ImmutableList<Pair<Color, ImmutableList<Vector3>>> paths) {
    for (int i = 0; i < paths.size(); i++) {
      Pair<Color, ImmutableList<Vector3>> path = paths.get(i);

      if (i != 0) {
        Pair<Color, ImmutableList<Vector3>> previousPath = paths.get(i - 1);

        if (!previousPath.getSecond().isEmpty() && !path.getSecond().isEmpty()) {
          renderPathInternal(
              path.getFirst(),
              ImmutableList.of(
                  Iterables.getLast(previousPath.getSecond()),
                  Objects.requireNonNull(Iterables.getFirst(path.getSecond(), Vector3.zero()))));
        }
      }

      renderPathInternal(path.getFirst(), path.getSecond());
    }
  }

  public void renderPath(ImmutableList<Vector3> path) {
    renderPath(Color.pink, path);
  }

  public void renderPath(Color color, ImmutableList<Vector3> path) {
    if (skipLineRendering()) {
      return;
    }

    renderPathInternal(color, path);
  }

  private void renderPathInternal(Color color, ImmutableList<Vector3> path) {
    Vector3 prev = null;
    for (Vector3 location : path) {
      if (prev != null) {
        getRenderer().drawLine3d(color, prev, location);
      }
      prev = location;
    }
  }

  public void renderPath(Color color, Path path) {
    if (skipLineRendering()) {
      return;
    }

    for (Segment segment : path.allNodes()) {
      switch (segment.type) {
        case FLIP:
        case JUMP:
        case STRAIGHT:
          render3DLine(color, segment.start, segment.end);
          break;
        case ARC:
          renderArc(color, segment);
          break;
      }
    }
  }

  public void renderPath(DataPacket input, Path path) {
    if (skipLineRendering()) {
      return;
    }

    ImmutableList<Segment> pathNodes = path.allNodes();

    for (int i = 0; i < pathNodes.size(); i++) {
      Segment segment = pathNodes.get(i);

      Color color = i == 0
          ? Color.white
          : pathNodes.get(i - 1).avgSpeed() < segment.avgSpeed() ? Color.GREEN : Color.RED;

      switch (segment.type) {
        case FLIP:
        case JUMP:
        case STRAIGHT:
          render3DLine(color, segment.start, segment.end);
          break;
        case ARC:
          renderArc(color, segment);
          break;
      }
    }

    renderPoint(Color.GREEN, path.pidTarget(input), 10);
    renderPoint(Color.ORANGE, path.currentTarget(input), 10);

    renderHitBox(Color.BLACK, path.getTarget());
  }

  public void renderRelativeProjection(Color color, CarData car, Vector2 projectedVector) {
    renderProjection(color, car, car.position.plus(projectedVector.asVector3()));
  }

  public void renderProjection(Color color, CarData car, Vector2 projectedVector) {
    renderProjection(color, car, projectedVector.asVector3());
  }

  public void renderProjection(Color color, CarData car, Vector3 projectedVector) {
    if (skipLineRendering()) {
      return;
    }

    // Render this frame only.
    renderProjection(car.position, projectedVector, color, car.elapsedSeconds);
  }

  public void renderProjection(CarData car, Vector2 projectedVector) {
    renderProjection(Color.CYAN, car, projectedVector.asVector3());
  }

  private Renderer getRenderer() {
    return BotLoopRenderer.forBotLoop(bot);
  }

  private void renderTurningRadius(DataPacket input) {
    if (input.car.position.z > 20) {
      return;
    }

    Paths.Circles radiusCircles = Paths.innerTurningRadiusCircles(input.car);

    if (radiusCircles.ccw.center.distance(input.car.position) < radiusCircles.cw.center.distance(input.car.position)) {
      renderCircle(Color.blue, radiusCircles.ccw);
    } else {
      renderCircle(Color.orange, radiusCircles.cw);
    }
  }

  public void renderHitBox(CarData car) {
    JumpManager jumpManager = car.isLiveData ? JumpManager.forCar(car) : JumpManager.copyForCar(car);
    Color color = jumpManager.canJump()
        ? Color.GREEN
        : jumpManager.canFlip()
        ? Color.ORANGE
        : !jumpManager.hasReleasedJumpInAir()
        ? Color.CYAN
        : Color.RED;
    renderHitBox(color, car);
  }

  private static final float COMPASS_SIZE = 100f;
  public void renderHitBox(Color color, CarData car) {
    if (skipLineRendering())
      return;

    BoundingBox hitbox = car.boundingBox;

    // Draw front box.
    render3DLine(color, hitbox.flt, hitbox.flb);
    render3DLine(color, hitbox.flb, hitbox.frb);
    render3DLine(color, hitbox.frb, hitbox.frt);
    render3DLine(color, hitbox.frt, hitbox.flt);

    // Draw Rear box.
    render3DLine(color, hitbox.rlt, hitbox.rlb);
    render3DLine(color, hitbox.rlb, hitbox.rrb);
    render3DLine(color, hitbox.rrb, hitbox.rrt);
    render3DLine(color, hitbox.rrt, hitbox.rlt);

    // Connect front and back.
    render3DLine(color, hitbox.flt, hitbox.rlt);
    render3DLine(color, hitbox.flb, hitbox.rlb);
    render3DLine(color, hitbox.frt, hitbox.rrt);
    render3DLine(color, hitbox.frb, hitbox.rrb);

    // Render compass
    render3DLine(Color.RED, car.position, car.position.plus(car.orientation.getNoseVector().toMagnitude(COMPASS_SIZE)));
    render3DLine(Color.GREEN, car.position, car.position.plus(car.orientation.getRightVector().toMagnitude(COMPASS_SIZE)));
    render3DLine(Color.BLUE, car.position, car.position.plus(car.orientation.getRoofVector().toMagnitude(COMPASS_SIZE)));
  }

  private static volatile int controllingBotRenderer = -1;

  private void renderStats(DataPacket input) {
    if (!GlobalDebugOptions.isRenderStats()) {
      return;
    }

    if (controllingBotRenderer == -1) {
      controllingBotRenderer = input.serialNumber;
    } else if (controllingBotRenderer != input.serialNumber) {
      return;
    }

    if (initialIngameTime == 0 || input.car.elapsedSeconds - initialIngameTime > 1) {
      float passedTime = input.car.elapsedSeconds - initialIngameTime;
      ingameFps = passedTime == 0 ? 0 : (ingameTicks / passedTime);
      initialIngameTime = input.car.elapsedSeconds;
      ingameTicks = 0;
    }
    ingameTicks++;

    if (initialWallClockTime == 0 || System.currentTimeMillis() - initialWallClockTime > 1000) {
      float passedTime = (float) (System.currentTimeMillis() - initialWallClockTime) / 1000;
      wallclockFps = passedTime == 0 ? 0 : wallClockTicks / passedTime;
      initialWallClockTime = System.currentTimeMillis();
      wallClockTicks = 0;
    }
    wallClockTicks++;

    renderText(400, 20, 1, "%s In-game FPS", ingameFps == 0 ? "-" : String.format("%.2f", ingameFps));
    renderText(400, 40, 1, "%s Wall-clock FPS", wallclockFps == 0 ? "-" : String.format("%.2f", wallclockFps));

    double averageComputeTime = ComputeTracker.averageSeconds(input.serialNumber) * 1000;
    renderText(400, 60, 1, "%sms Avg compute time", averageComputeTime == 0 ? "-" : String.format("%.4f", averageComputeTime));
  }

  private void renderDebugText() {
    Renderer renderer = getRenderer();

    for (int i = 0; i < renderList.size(); i++) {
      RenderedString string = renderList.get(i);
      renderer.drawString2d(string.text, string.color, new Point(TEXT_LIST_START_X, TEXT_LIST_START_Y + (TEXT_LIST_SPACING_Y * i)), 2, 2);
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
  private Color[] alertColors = {Color.PINK, Color.WHITE, Color.CYAN};
  private String alertText;
  private int alertColor;

  public void addAlertText(String alertText, Object... args) {
    this.alertText = String.format(alertText, args);
    this.alertColor = ++alertColor % alertColors.length;
  }

  private void renderAlert(DataPacket input) {
    if (alertText != null && alertTimeSeconds == 0) {
      alertTimeSeconds = input.car.elapsedSeconds;
    } else if (input.car.elapsedSeconds - alertTimeSeconds > 2) {
      alertTimeSeconds = 0;
      alertText = null;
    }

    if (alertText != null) {
      renderText(alertColors[alertColor], 800, 400, 3, alertText);
    }
  }

  private float lastTouch = -2;

  public void setTouchIndicator(DataPacket input) {
    lastTouch = input.car.elapsedSeconds;
  }

  private void renderTouchIndicator(DataPacket input) {
    if (input.car.elapsedSeconds < lastTouch + .01) {
      render3DSquare(Color.RED, input.ball.position, Constants.BALL_RADIUS * 1.5f);
    }

    if (nearestHitboxPoint != null) {
      render3DLine(Color.PINK, input.car.position, nearestHitboxPoint);
    }
  }

  public void renderPoint(Color pink, Vector3 location, int size) {
    getRenderer().drawRectangle3d(pink, location, size, size, true);
  }

  public void setIntersectionTarget(Vector3 target) {
    this.intersectionTarget = target;
  }

  private Vector3 nearestHitboxPoint;
  public void setNearestHitboxPoint(Vector3 nearestHitboxPoint) {
    this.nearestHitboxPoint = nearestHitboxPoint;
  }

  private static final float BALL_PREDICTION_TIME = 3.0f;
  private BallData prediction;
  private BallData actual;
  private ImmutableList<BallData> predictionTrace;
  public void setPredictionDiff(BallData prediction, BallData actual) {
    this.prediction = prediction;
    this.predictionTrace = BallPredictor.makePrediction(prediction);
    this.actual = actual;
  }

  private void renderPredictionDiff(DataPacket input) {
    if (prediction == null || actual == null) {
      return;
    }

    renderText(0, 490, "vX: %d", (int) (prediction.velocity.x - actual.velocity.x));
    renderText(0, 520, "vY: %d", (int) (prediction.velocity.y - actual.velocity.y));
    renderText(0, 550, "vZ: %d", (int) (prediction.velocity.z - actual.velocity.z));

    renderText(0, 400, "sX: %d", (int) (prediction.spin.x - actual.spin.x));
    renderText(0, 430, "sY: %d", (int) (prediction.spin.y - actual.spin.y));
    renderText(0, 460, "sZ: %d", (int) (prediction.spin.z - actual.spin.z));

    renderText(0, 580, "dS: %d", (int) prediction.spin.minus(actual.spin).magnitude());
    renderText(0, 610, "dP: %d", (int) prediction.velocity.minus(actual.velocity).magnitude());
    renderText(0, 640, "%% %.2f", (prediction.velocity.minus(actual.velocity).magnitude() * 100
        / prediction.velocity.magnitude()));

    renderBallPrediction(predictionTrace);

    if (prediction.time + BALL_PREDICTION_TIME < input.car.elapsedSeconds) {
      prediction = null;
      actual = null;
      predictionTrace = null;
    }
  }

  public void renderBallPrediction(BallData projectedBallData) {
    renderBallPrediction(BallPredictor.makePrediction(projectedBallData));
  }

  public void renderBallPrediction(ImmutableList<BallData> trace) {
    BallData previousPrediction = null;
    for (BallData nextPrediction : trace) {
      if (previousPrediction == null) {
        previousPrediction = nextPrediction;
      } else if (nextPrediction.time - previousPrediction.time > .1) {
        render3DLine(Color.white, previousPrediction.position, nextPrediction.position);
        previousPrediction = nextPrediction;
      }
    }
    getRenderer().drawString3d("Eru", Color.white, previousPrediction.position, 2, 2);
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
  private Vector3 carTarget;
  private Vector3 tacticObject;
  private Vector3 intersectionTarget;

  public void setCarTarget(Vector3 target) {
    this.carTarget = target;
  }

  private void renderTacticLines(CarData car) {
    if (carTarget != null) {
      render3DLine(Color.RED, car.position, carTarget);
    }

    if (carTarget != null && tacticObject != null) {
      render3DLine(Color.ORANGE, carTarget, tacticObject);
    }

    if (intersectionTarget != null) {
      render3DSquare(Color.ORANGE, intersectionTarget, 30);
    }
  }

  private void renderControlDebug() {
    renderText(Color.PINK, 0, 300, "%s", strategist == null ? "NONE" : strategist.getType());
    renderText(Color.CYAN, 150, 300, "%s", tactic == null ? "NONE" : tactic.tacticType);
    renderText(Color.PINK, 400, 300, "%s",
        tactician == null ? "NONE" : tactician.getClass().getSimpleName().replace("Tactician", ""));
    renderText(Color.CYAN, 0, 270, "%s", branch);

    if (tactician != null && tactician.isLocked()) {
      renderText(Color.MAGENTA, 400, 270, "LOCKED");
    }

    logger.log(Level.DEBUG, "{} {} {} {}",
        strategist == null ? "NONE" : strategist.getType(),
        tactic == null ? "NONE" : tactic.tacticType,
        tactician == null ? "NONE" : tactician.getClass().getSimpleName().replace("Tactician", ""),
        branch);
  }

  public void setStrategy(Strategist strategist) {
    this.strategist = strategist;
  }

  public void setTactic(Tactic tactic) {
    this.tactic = tactic;
    this.carTarget = tactic.getTargetPosition();
    this.tacticObject = tactic.object;
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

  private String formatTime(float elapsedSeconds) {
    int minutes = ((int) elapsedSeconds) / 60;
    float seconds = elapsedSeconds % 60;
    return String.format("%d:%#.2f", minutes, seconds);
  }

  private void renderControls(Controls output) {
    renderText(250, 340, String.format("Throttle %.2f", output.getThrottle()));
    renderText(250, 370, String.format("Turn %.2f %s", output.getSteer(), lor(output.getSteer())));
    renderText(250, 400, String.format("Boost %s", output.holdBoost()));
    renderText(250, 430, String.format("Drift %s", output.holdHandbrake()));
    renderText(250, 460, String.format("Jump %s", output.holdJump()));
    renderText(250, 490, String.format("Pitch %.2f %s", output.getPitch(), ud(output.getPitch())));
    renderText(250, 520, String.format("Yaw %.2f %s", output.getYaw(), lor(output.getYaw())));
    renderText(250, 550, String.format("Roll %.2f %s", output.getRoll(), lor(output.getRoll())));
  }

  private void renderBallVel(DataPacket input) {
    BallData ball = input.ball;

    renderText(0, 400, String.format("bV3: %.0f", ball.velocity.magnitude()));
    renderText(0, 430, String.format("bV2: %.0f", ball.velocity.flat().magnitude()));
  }

  private void renderCarLocation(DataPacket input) {
    CarData car = input.car;

    renderText(0, 400, String.format("X: %.0f", car.position.x));
    renderText(0, 430, String.format("Y: %.0f", car.position.y));
    renderText(0, 460, String.format("Z: %.0f", car.position.z));
  }

  private void renderCarAccel(DataPacket input) {
    CarData car = input.car;

    if (previousVelocities.size() == SMOOTHING_INTERVAL) {

      double deltaV = previousVelocities.peekLast().minus(previousVelocities.peekFirst()).flatten().magnitude();
      double deltaT = previousVelocityTimes.peekLast() - previousVelocityTimes.peekFirst();

      int speed = (int) car.velocity.magnitude();
      // Delta V / Delta T
      int acceleration = (int) (deltaV / deltaT);

      renderText(0, 340, String.format("Vel: %d", speed));
      renderText(0, 370, String.format("Acc: %d", acceleration));

      previousVelocities.removeFirst();
      previousVelocityTimes.removeFirst();
    }

    previousVelocities.add(car.velocity);
    previousVelocityTimes.add(car.elapsedSeconds);
  }

  private void renderRelativeBallData(DataPacket input) {
    BallData relativeBallData = RelativeUtils.noseRelativeBall(input);

    renderText(0, 400, "Z: %d", (int) relativeBallData.position.z);
    renderText(0, 430, "Y: %d", (int) relativeBallData.position.y);
    renderText(0, 460, "X: %d", (int) relativeBallData.position.x);

    renderText(0, 490, "dZ: %d", (int) relativeBallData.velocity.z);
    renderText(0, 520, "dY: %d", (int) relativeBallData.velocity.y);
    renderText(0, 550, "dX: %d", (int) relativeBallData.velocity.x);
  }

  private void renderArc(Color color, Segment arc) {
    Preconditions.checkArgument(arc.type == Segment.Type.ARC, "Segment must be of type ARC");
    renderCircle(color, arc.circle.center, arc.start, arc.circle.radius, arc.getRadians());
  }

  public void renderCircle(Color color, Circle circle) {
    renderCircle(color, circle.center, circle.radius);
  }

  private void renderCircle(Color color, Vector3 center, double radius) {
    renderCircle(color, center, center.plus(Vector3.of(radius, 0, 0)), radius, FULL_CIRCLE);
  }

  private void renderCircle(Color color, Vector3 center, Vector3 startPoint, double radius, double radians) {
    ImmutableList<Vector3> points = toCirclePoints(center, startPoint, radius, radians);

    Vector3 prevPoint = null;
    for (Vector3 nextPoint : points) {
      if (prevPoint != null) {
        render3DLine(color, prevPoint, nextPoint);
      }
      prevPoint = nextPoint;
    }

    // Close the loop.
    if (prevPoint != null && radians == FULL_CIRCLE) {
      render3DLine(color, prevPoint, points.get(0));
    }
  }

  private static final int POINT_COUNT = 20;
  private ImmutableList<Vector3> toCirclePoints(Vector3 center, Vector3 initialPoint, double radius, double radians) {
    Vector2 ray = initialPoint.minus(center).flatten();
    double radianOffset = Vector2.WEST.correctionAngle(ray);

    int renderPoints = Math.min(POINT_COUNT, (int) (Math.abs(radians) * 10));
    ImmutableList<Vector3> curve = IntStream.range(1, renderPoints)
        .mapToDouble(index -> index * radians / renderPoints) // Map to angle radians
        .mapToObj(nextRadian -> Circle.pointOnCircle(center, radius, nextRadian + radianOffset))
        .collect(toImmutableList());

    return ImmutableList.<Vector3>builder()
        .add(initialPoint) // First point
        .addAll(curve)
        .add(Circle.pointOnCircle(center, radius, radians + radianOffset)) // Last point
        .build();
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
    renderText(color, x, y, 2, text, args);
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

  private void render3DLine(Color color, Vector3 loc1, Vector3 loc2) {
    getRenderer().drawLine3d(color, loc1, loc2);
  }

  private void render3DSquare(Color color, Vector3 center, float sideLength) {
    getRenderer().drawCenteredRectangle3d(
        color,
        center,
        (int) sideLength,
        (int) sideLength,
        false);
  }
}
