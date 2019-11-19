from math import pi

from kickoff.kickoff_training import KickOff

kickoff_exercises = [
    # KickOff('Center Kickoff', car_start_x=0, car_start_y=-4608, car_yaw=(.5 * pi)),
    # KickOff('Left Kickoff', car_start_x=2048, car_start_y=-2560, car_yaw=(.75 * pi)),
    KickOff('Left Center Kickoff', car_start_x=256, car_start_y=-3840, car_yaw=(.5 * pi)),
    # KickOff('Right Center Kickoff', car_start_x=-256, car_start_y=-3840, car_yaw=(.5 * pi)),
    # KickOff('Right Kickoff', car_start_x=-2048, car_start_y=-2560, car_yaw=(.25 * pi)),
]