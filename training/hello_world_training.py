from pathlib import Path

from rlbot.matchconfig.match_config import PlayerConfig, Team

from bronze.bronze_exercises import bronze_exercises
from dribble.dribble_exercises import dribble_exercises
from kickoff.kickoff_exercises import kickoff_exercises
from stone.stone_exercises import stone_exercises
from wavedash.wavedash_exercises import wavedash_exercises


def make_default_playlist():
    exercises = []
    # exercises += stone_exercises
    # exercises += bronze_exercises
    exercises += dribble_exercises
    # exercises += kickoff_exercises
    # exercises += wavedash_exercises

    for exercise in exercises:
        exercise.match_config.player_configs = [
            PlayerConfig.bot_config(
                Path(__file__).absolute().parent.parent / 'src' / 'main' / 'python' / 'ut_version.cfg',
                Team.BLUE)
        ]

    return exercises
