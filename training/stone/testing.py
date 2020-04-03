from math import pi

from stone.stone_shot_on_goal import Setup

# Sticking the car to the wall...
testing_locations = [
    # Setup("Orange Left Wall facing up", car_x=4079, car_y=3650, car_z=500, car_spin=0, car_pitch=pi/2),
    # Setup("Orange Right Wall facing up", car_x=-4079, car_y=3650, car_z=500, car_spin=pi, car_pitch=pi/2),
    # Setup("Orange Right Wall facing blue goal", car_x=-4079, car_y=3650, car_z=500, car_spin=-pi/2, car_roll=pi/2),
    # Setup("Orange Right Wall facing center", car_x=-4079, car_y=3650, car_z=500, car_spin=-pi/2, car_roll=pi/2,
    #       car_pitch=-pi/4),

    # Setup("Blue Left Wall facing up", car_x=4079, car_y=-3650, car_z=500, car_spin=0, car_pitch=pi/2),
    # Setup("Blue Right Wall facing up", car_x=-4079, car_y=-3650, car_z=500, car_spin=pi, car_pitch=pi/2),

    # Setup("Corner", ball_x=1500, ball_y=1750, ball_z=100, car_x=3480, car_y=4560, car_z=500, car_spin=pi/4, car_pitch=pi/2),
    # Setup("Center", ball_y=5000, ball_z=100, car_x=0, car_y=-4000, car_z=20, car_vy=0, car_spin=-pi/2),

    Setup("Wait ...", ball_x=0, ball_y=-3500, ball_z=1500, car_x=0, car_y=-5000, car_z=20, car_vy=0, car_spin=pi / 2),
    # Setup("Center", ball_x=0, ball_y=-3500, ball_z=1500, car_x=0, car_y=-5000, car_z=20, car_vy=0, car_spin=pi/2),
    # Setup("Center", ball_y=-0, ball_z=100, ball_vx=-100, ball_vy=-1500, ball_vz=1000, car_x=0, car_y=-5000, car_z=20, car_vy=0, car_spin=pi/2),
]
