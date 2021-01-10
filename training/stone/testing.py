from math import pi

from stone.stone_shot_on_goal import Setup, Recover

# TODO: Make it so these also need to save a shot or something that doesn't work if you don't land well
recovery_tests = [
    Recover("Driving", car_x=2000, car_y=3000, car_z=0, car_vy=1500, car_vz=0, car_pitch=0),

    # Recover("Flat landing floor",
    #         car_y=-3000, car_z=300, car_spin=pi / 2),
    # # TODO: This doesn't work for some reason. When in just the right orientation, it won't rotate.
    # Recover("Flat landing ceiling",
    #         car_y=-3000, car_z=300, car_vz=1600, car_spin=pi / 2),
    # Recover("Flat landing sideways momentum",
    #         car_y=-3000, car_z=100, car_vy=1500, car_spin=-pi / 2, car_pitch=0, car_roll=pi),
    # Recover("Flat landing the ground",
    #         car_x=-2000, car_y=-3000, car_z=500, car_vx=1500, car_vz=700),
    # Recover("Flat landing on left wall",
    #         car_x=2000, car_y=-3000, car_z=500, car_vx=1500, car_vz=700),
    # Recover("Flat landing on right wall",
    #         car_x=-2000, car_y=-3000, car_z=500, car_vx=-1500, car_vz=700),
    # Recover("Flat landing on front wall",
    # car_x=2000, car_y=3000, car_z=500, car_vy=1500, car_vz=700, car_pitch=0),
    # Recover("Flat landing on back wall",
    #         car_x=2000, car_y=-3000, car_z=500, car_vy=-1500, car_vz=700),
    # Recover("Flat landing on back-left corner",
    #         car_x=2000, car_y=-3000, car_z=500, car_vx=1500, car_vy=-1500, car_vz=700),
    # Recover("Flat landing on back-right corner",
    #         car_x=-2000, car_y=-3000, car_z=500, car_vx=-1500, car_vy=-1500, car_vz=700),
    # Recover("Flat landing on front-left corner",
    #         car_x=2000, car_y=3000, car_z=500, car_vx=1500, car_vy=1500, car_vz=700),
    # Recover("Flat landing on front-right corner",
    #         car_x=-2000, car_y=3000, car_z=500, car_vx=-1500, car_vy=1500, car_vz=700),
    #
    # Recover("fast upfield landing on left wall", car_x=3000, car_y=-3000, car_z=500, car_vx=500, car_vy=2000,
    #         car_vz=1200),
    #
    # Recover("fast up-field boost to roof", car_x=3000, car_y=-3000, car_z=500, car_vx=0, car_vy=2000, car_vz=1200),
    # Recover("fast up-field boost to left wall", car_x=3500, car_y=-3700, car_z=100, car_vx=0, car_vy=1000, car_vz=1200),
    #
    # Recover("fast backfield landing in back-left corner", car_x=3000, car_y=-100, car_z=500, car_vx=500, car_vy=-2000,
    #         car_vz=1200),
    # Recover("Into Blue Goal side", car_x=1000, car_y=-4000, car_z=300, car_vx=-1100, car_vy=-1000, car_vz=600,
    #         car_spin=pi / 2),
    # Recover("Into Blue Goal side", car_x=600, car_y=-4000, car_z=300, car_vx=-200, car_vy=-1000, car_vz=600,
    #         car_spin=pi / 2),
    # Recover("Off Blue cross-bar", car_x=1000, car_y=-4000, car_z=300, car_vx=-1000, car_vy=-1000, car_vz=700,
    #         car_spin=pi / 2),
]

air_dribbling = [
    Setup("Air Dribbling", ball_z=700, ball_vz=200, car_z=500, car_vz=200, car_pitch=pi / 2)
]

wall_sticking_tests = [
    # Setup("Orange Left Wall facing up", car_x=4079, car_y=3650, car_z=500, car_spin=0, car_pitch=pi/2),
    # Setup("Orange Right Wall facing up", car_x=-4079, car_y=3650, car_z=500, car_spin=pi, car_pitch=pi/2),
    # Setup("Orange Right Wall facing blue goal", car_x=-4079, car_y=3650, car_z=500, car_spin=-pi/2, car_roll=pi/2),
    # Setup("Orange Right Wall facing center", car_x=-4079, car_y=3650, car_z=500, car_spin=-pi/2, car_roll=pi/2,
    #       car_pitch=-pi/4),

    # Setup("Blue Left Wall facing up", car_x=4079, car_y=-3650, car_z=500, car_spin=0, car_pitch=pi/2),
    # Setup("Blue Right Wall facing up", car_x=-4079, car_y=-3650, car_z=500, car_spin=pi, car_pitch=pi/2),

    # Setup("Corner", ball_x=1500, ball_y=1750, ball_z=100, car_x=3480, car_y=4560, car_z=500, car_spin=pi/4, car_pitch=pi/2),
    # Setup("Center", ball_y=5000, ball_z=100, car_x=0, car_y=-4000, car_z=20, car_vy=0, car_spin=-pi/2),

    # Setup("Rotate", ball_x=-2000, ball_y=-4000, ball_z=100, car_x=0, car_y=4000, car_z=20, car_vy=0, car_spin=pi),
    # Setup("Rotate", ball_x=-2000, ball_y=-4000, ball_z=100, car_x=3000, car_y=-1000, car_z=20, car_vy=0, car_spin=-pi/4, boost=40),
]

aerialing_tests = [

    # Setup("Jump Ball", ball_x=0, ball_y=0, ball_z=100, ball_vz=1500, car_x=0, car_y=-3000, car_z=20, car_vy=0, car_spin=pi/2, boost=100),
    # Setup("In-air only", ball_x=3000, ball_vx=-1000, ball_y=0, ball_z=100, ball_vz=1500, car_x=0, car_y=-3000,
    #       car_z=200, car_vx=00, car_vy=100, car_vz=1000, car_spin=pi/2, boost=100),
    # Setup("Down-field pass: In-air only",
    #       ball_x=2000, ball_vx=-1000, ball_y=0, ball_vy=1000, ball_z=100, ball_vz=1500,
    #       car_x=-2000, car_y=0, car_z=200, car_vx=00, car_vy=100, car_vz=1000, car_spin=pi/2, boost=100),

    # Setup("Redirect pass: In-air only",
    #       ball_x=2000, ball_vx=-1000, ball_y=0, ball_vy=1000, ball_z=100, ball_vz=1500,
    #       car_x=-3000, car_y=3000, car_z=200, car_vx=00, car_vy=100, car_vz=1000, car_spin=0, boost=100),
]

# Sticking the car to the wall...
testing_locations = [
                        # Setup("Ball rolling across Ball", ball_x=0, ball_y=0, ball_z=100, ball_vx=600, ball_vz=0, ball_vy=100, car_x=0, car_y=-3000, car_z=20, car_vy=0, car_spin=pi/2, boost=100),

                        # Setup("Rotate", ball_x=-2000, ball_y=-4000, ball_z=100, car_x=2500, car_y=-2000, car_z=20, car_vy=0, car_spin=-pi/1.5),

                        # Setup("Wait ...", ball_x=0, ball_y=-3500, ball_z=1500, car_x=0, car_y=-5000, car_z=20, car_vy=0, car_spin=pi / 2),
                        # Setup("Half flip", ball_x=0, ball_y=-1000, ball_z=100, car_x=0, car_y=-5000, car_z=20, car_vy=0, car_spin=-pi / 2),
                        # Setup("Center", ball_x=0, ball_y=-3500, ball_z=1500, car_x=0, car_y=-5000, car_z=20, car_vy=0, car_spin=pi/2),
                        # Setup("Center", ball_y=-0, ball_z=100, ball_vx=-100, ball_vy=-1500, ball_vz=1000, car_x=0, car_y=-5000, car_z=20, car_vy=0, car_spin=pi/2),
                    ] \
                    + air_dribbling
# + recovery_tests

orange_testing_locations = [
    Setup("Orange Rotate", ball_x=2000, ball_y=4000, ball_z=100, car_x=4000, car_y=500, car_z=20, car_vy=0,
          car_spin=pi * .49, boost=40),
]
