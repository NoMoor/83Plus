from pathlib import Path

from rlbot.matchconfig.match_config import PlayerConfig, Team

from aerials.aerial_exercises import aerial_exercises
from bronze.bronze_exercises import bronze_exercises
from dribble.dribble_exercises import dribble_exercises
from kickoff.kickoff_exercises import kickoff_exercises
from stone.stone_exercises import stone_exercises
from stone.stone_shot_on_goal import make_default_playlist as stone_shots
from wavedash.wavedash_exercises import wavedash_exercises
from rlbottraining.common_exercises.bronze_striker import make_default_playlist as common_bronze_striker
from rlbottraining.common_exercises.bronze_goalie import make_default_playlist as common_bronze_goalie
from rlbottraining.common_exercises.silver_striker import make_default_playlist as common_silver_striker
from rlbottraining.common_exercises.silver_goalie import make_default_playlist as common_silver_goalie


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
                Path(__file__).absolute().parent.parent / 'src' / 'main' / 'python' / 'dev_version.cfg',
                Team.BLUE)
        ]

    return exercises
