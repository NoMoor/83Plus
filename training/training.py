from rlbottraining.common_exercises import bronze_striker as common_bronze_striker

from agc_player_config import agc_blue
from bronze.bronze_exercises import bronze_exercises


def make_default_playlist():
    exercises = []

    exercises += bronze_exercises

    # exercises += stone_exercises
    # exercises += bronze_exercises
    # exercises += aerial_exercises
    # exercises += dribble_exercises
    # exercises += kickoff_exercises
    # exercises += wavedash_exercises
    # exercises += rebound_exercises
    exercises += common_bronze_striker.make_default_playlist()
    # exercises += common_bronze_goalie()
    # exercises += common_silver_striker()
    # exercises += common_silver_goalie()

    for exercise in exercises:
        exercise.match_config.player_configs = [agc_blue]
        exercise.match_config.enable_lockstep = False

    return exercises
