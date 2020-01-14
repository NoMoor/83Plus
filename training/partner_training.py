from pathlib import Path

from rlbot.matchconfig.match_config import PlayerConfig, Team
from twop.onevone import onevone_exercises


def make_default_playlist():
    exercises = []
    exercises += onevone_exercises

    for exercise in exercises:
        exercise.match_config.player_configs = [
            PlayerConfig.bot_config(
                Path(__file__).absolute().parent.parent / 'src' / 'main' / 'python' / 'local_version.cfg',
                Team.BLUE),
            PlayerConfig.bot_config(
                Path(__file__).absolute().parent.parent / 'src' / 'main' / 'python' / 'local_version.cfg',
                Team.ORANGE)
        ]

    return exercises
