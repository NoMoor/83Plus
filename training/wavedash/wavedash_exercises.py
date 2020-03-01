from math import pi

from wavedash.speed_training import FarAwayBall

wavedash_exercises = [
    FarAwayBall('Full Boost', boost=100),
    FarAwayBall('No Boost', boost=0),
    FarAwayBall('Sideways', car_z=100, car_vy=1000, car_spin=0),
    FarAwayBall('Sideways with speed', car_z=100, car_vy=1000, car_vx=1000, car_spin=0),
    FarAwayBall('Sideways with speed', car_z=100, car_vy=1000, car_vx=-1000, car_spin=0),
    FarAwayBall('Sideways with speed', car_z=100, car_vy=1000, car_vx=-1000, car_spin=-pi / 2),
    FarAwayBall('Sideways with speed', car_x=0, car_z=100, car_vy=1000, car_spin=-pi / 2),
]
