from agc_player_config import agc_blue
from manticore_player_config import manticore_blue, manticore_orange
from relief_player_config import relief_blue, relief_orange
from threevthree.threevthree import threevthree_exercises


def make_default_playlist():
    exercises = []
    exercises += threevthree_exercises

    for exercise in exercises:
        exercise.match_config.player_configs = [agc_blue, relief_blue, manticore_blue, relief_orange, relief_orange,
                                                manticore_orange]
        exercise.match_config.enable_lockstep = False

    return exercises
