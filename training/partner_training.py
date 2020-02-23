from agc_player_config import agc_blue, agc_orange
from kickoff.kickoff_exercises import kickoff_1v1_exercises


def make_default_playlist():
    exercises = []
    # exercises += onevone_exercises
    exercises += kickoff_1v1_exercises

    for exercise in exercises:
        exercise.match_config.player_configs = [agc_blue, agc_orange]
        exercise.match_config.enable_lockstep = True

    return exercises
