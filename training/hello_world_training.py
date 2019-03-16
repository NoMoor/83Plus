from pathlib import Path

from rlbot.matchconfig.match_config import PlayerConfig, Team

from bronze.bronze_exercises import bronze_exercises
from stone.stone_exercises import stone_exercises


def make_default_playlist():
    exercises = []
    exercises += stone_exercises
    # exercises += bronze_exercises

    for exercise in exercises:
        exercise.match_config.player_configs = [
            PlayerConfig.bot_config(
                Path(__file__).absolute().parent.parent / 'src' / 'main' / 'python' / 'botv1.cfg',
                Team.BLUE)
        ]

    return exercises
