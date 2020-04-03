from math import pi

from threevthree.threevthreerotations import Defending

threevthree_exercises = [
    Defending(
        "Aerial Shot",
        ball_p=[0, -2000, 100],
        ball_v=[200, -1000, 0],
        test_car_p=[0, -5300],
        car_1_p=[3000, 5000],
        car_1_yaw=-pi/2,
        car_2_p=[-3000, 5000],
        car_2_yaw=-pi/2,
        car_3_p=[-0, 2000],
        car_4_p=[1000, 3000],
        car_5_p=[-1000, 3000])
]
