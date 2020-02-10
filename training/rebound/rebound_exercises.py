from math import pi

# from training.agc_player_config import agc_config
from rebound.rebound_scenarios import Rebound

rebound_exercises = [
    # Rebound('Left Post Rebound', ball_y=200, ball_vx=460, ball_vy=2500, ball_vz=400, car_x=0, car_y=0, car_yaw=(pi/2),
    #         car_vy=1000),
    # Rebound('Right Post Rebound', ball_y=200, ball_vx=-460, ball_vy=2500, ball_vz=400, car_x=0, car_y=0, car_yaw=(pi/2),
    #         car_vy=1000),
    Rebound('Backboard Rebound', ball_y=200, ball_vy=2500, ball_vz=1000, car_x=0, car_y=0, car_yaw=(pi / 2),
            car_vy=1000),
]

# def make_default_playlist():
#     for exercise in kickoff_exercises:
#         exercise.match_config.player_configs = [agc_config]
#
#     return kickoff_exercises
