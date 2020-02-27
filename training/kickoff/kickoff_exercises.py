from math import pi

from kickoff.kickoff_training import KickOff, KickOff1v1, KickOffOrange

kickoff_exercises = [
    KickOff('Center Kickoff', car_start_x=0, car_start_y=-4608, car_yaw=(.5 * pi)),
    # KickOff('Left Center Kickoff', car_start_x=256, car_start_y=-3840, car_yaw=(.5 * pi)),
    # KickOff('Right Center Kickoff', car_start_x=-256, car_start_y=-3840, car_yaw=(.5 * pi)),
    # KickOff('Left Kickoff', car_start_x=2048, car_start_y=-2560, car_yaw=(.75 * pi)),
    # KickOff('Right Kickoff', car_start_x=-2048, car_start_y=-2560, car_yaw=(.25 * pi)),
]

kickoff_orange_exercises = [
    # KickOffOrange('Center Kickoff', car_start_x=0, car_start_y=-4608, car_yaw=(.5 * pi)),
    # KickOffOrange('Left Center Kickoff', car_start_x=256, car_start_y=-3840, car_yaw=(.5 * pi)),
    # KickOffOrange('Right Center Kickoff', car_start_x=-256, car_start_y=-3840, car_yaw=(.5 * pi)),
    # KickOffOrange('Left Kickoff', car_start_x=2048, car_start_y=-2560, car_yaw=(.75 * pi)),
    KickOffOrange('Right Kickoff', car_start_x=-2048, car_start_y=-2560, car_yaw=(.25 * pi)),
]

kickoff_1v1_exercises = [
    # KickOff1v1('Center Kickoff', car_start_x=0, car_start_y=-4608, car_yaw=(.5 * pi)),
    # KickOff1v1('Left Center Kickoff', car_start_x=256, car_start_y=-3840, car_yaw=(.5 * pi)),
    # KickOff1v1('Right Center Kickoff', car_start_x=-256, car_start_y=-3840, car_yaw=(.5 * pi)),
    # KickOff1v1('Left Kickoff', car_start_x=2048, car_start_y=-2560, car_yaw=(.75 * pi)),
    KickOff1v1('Right Kickoff', car_start_x=-2048, car_start_y=-2560, car_yaw=(.25 * pi)),
]
