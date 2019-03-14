from pathlib import Path

from rlbottraining.common_exercises.bronze_goalie import BallRollingToGoalie
from rlbottraining.common_exercises.bronze_striker import BallInFrontOfGoal
from rlbottraining.common_exercises.silver_striker import HookShot
from rlbot.matchconfig.match_config import PlayerConfig, Team

from goalie_jump_training import BallFloatingIntoGoal
from fast_rolling_goalie import BallRollingToGoal


def make_default_playlist():
    exercises = [
        # BallFloatingIntoGoal('Test'),
        # BallRollingToGoalie('Hello Exercise world!'),
        BallRollingToGoal('Test'),
        # BallInFrontOfGoal('Striker'),
        # HookShot('Test'),
    ]

    for exercise in exercises:
        exercise.match_config.player_configs = [
            PlayerConfig.bot_config(
                Path(__file__).absolute().parent.parent / 'src' / 'main' / 'python' / 'botv1.cfg',
                Team.BLUE)
        ]

    return exercises