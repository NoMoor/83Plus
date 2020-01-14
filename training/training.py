from pathlib import Path

from rlbot.matchconfig.match_config import PlayerConfig, Team
from stone.stone_exercises import stone_exercises


def make_default_playlist():
    exercises = []
    # exercises += aerial_exercises
    exercises += stone_exercises
    # exercises += bronze_exercises
    # exercises += dribble_exercises
    # exercises += kickoff_exercises
    # exercises += wavedash_exercises

    # exercises += common_bronze_striker()
    # exercises += common_bronze_goalie()
    # exercises += common_silver_striker()
    # exercises += common_silver_goalie()

    for exercise in exercises:
        exercise.match_config.player_configs = [
            PlayerConfig.bot_config(
                Path(__file__).absolute().parent.parent / 'src' / 'main' / 'python' / 'local_version.cfg',
                Team.BLUE)
        ]
        exercise.match_config.mutators.gravity = "Low"

    return exercises
