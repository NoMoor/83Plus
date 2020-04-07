from math import pi

from stone.stone_shot_on_goal import Defense

facing_orange = pi / 2
facing_left = 0
facing_back_left = -pi / 4

stone_defense_exercises = [
    Defense("Sitting in goal",
            ball_x=0, ball_y=-2000, ball_vx=400, ball_vy=-2000,
            car_y=-5000, car_z=20, car_spin=facing_orange),
    Defense("Sitting in right goal far post",
            ball_x=1000, ball_y=-2000, ball_vx=-1000, ball_vy=-1800,
            car_x=750, car_y=-5500, car_z=20, car_spin=facing_orange),
    Defense("Sitting in right goal near post",
            ball_x=2500, ball_y=-3000, ball_vx=-1500, ball_vy=-1800, ball_vz=500,
            car_x=750, car_y=-5500, car_z=20, car_spin=facing_orange),
    Defense("Sitting in goal",
            ball_x=0, ball_y=-2000, ball_vy=-2000, ball_vz=750,
            car_y=-5000, car_z=20, car_spin=facing_orange),
    Defense("From the side rolling",
            ball_x=0, ball_y=-2000, ball_vy=-2000,
            car_x=-2000, car_y=-4800, car_z=20, car_spin=facing_left),
    Defense("From the side bouncing",
            ball_x=0, ball_y=-1000, ball_vy=-1000, ball_vz=1000,
            car_x=-2000, car_y=-4800, car_z=20, car_spin=facing_left),
    Defense("Chasing the side rolling",
            ball_x=0, ball_y=-2000, ball_vy=-2000,
            car_x=-1000, car_y=-4000, car_z=20, car_spin=facing_back_left),
    Defense("Chasing the side bouncing",
            ball_x=0, ball_y=-1000, ball_vy=-1000, ball_vz=1000,
            car_x=-3000, car_y=-2000, car_z=20, car_spin=facing_back_left),
]
