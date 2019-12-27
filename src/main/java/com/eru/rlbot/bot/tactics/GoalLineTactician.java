package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.common.Path;
import com.eru.rlbot.bot.common.PathPlanner;
import com.eru.rlbot.bot.main.Agc;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

public class GoalLineTactician extends Tactician {

  GoalLineTactician(Agc bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  @Override
  public void execute(DataPacket input, ControlsOutput output, Tactic tactic) {
    // TODO: If this has an object, we should hit toward that.
    Path planPath = PathPlanner.doDefensePlanning(input);

    bot.botRenderer.renderPath(planPath);
    pathExecutor.executePath(input, output, planPath);
  }
}
