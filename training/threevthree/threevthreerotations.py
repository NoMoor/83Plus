from dataclasses import dataclass, field
from typing import List

from math import pi
from rlbot.utils.game_state_util import GameState, BallState, CarState, Physics, Vector3, Rotator
from rlbottraining.common_graders.goal_grader import GoalieGrader
from rlbottraining.grading.grader import Grader
from rlbottraining.rng import SeededRandomNumberGenerator
from rlbottraining.training_exercise import TrainingExercise


def make_grader():
    return GoalieGrader(timeout_seconds=10.0)


zero = Vector3(0, 0, 0)
car_at_rest = 17


@dataclass
class Defending(TrainingExercise):
    grader: Grader = field(default_factory=make_grader)
    ball_p: List[float] = field(default_factory=lambda: [0, 0, 92])
    ball_v: List[float] = field(default_factory=lambda: [0, 0, 0])
    test_car_p: List[float] = field(default_factory=lambda: [0, -1000, 17])
    test_car_yaw: float = pi / 2
    car_1_p: List[float] = field(default_factory=lambda: [1000, -1000, 17])
    car_1_yaw: float = pi / 2
    car_2_p: List[float] = field(default_factory=lambda: [-1000, -1000, 17])
    car_2_yaw: float = pi / 2
    car_3_p: List[float] = field(default_factory=lambda: [0, 1000, 17])
    car_3_yaw: float = -pi / 2
    car_4_p: List[float] = field(default_factory=lambda: [1000, 1000, 17])
    car_4_yaw: float = -pi / 2
    car_5_p: List[float] = field(default_factory=lambda: [-1000, 1000, 17])
    car_5_yaw: float = -pi / 2

    def make_game_state(self, rng: SeededRandomNumberGenerator) -> GameState:
        return GameState(
            ball=BallState(physics=Physics(
                location=Vector3(self.ball_p[0], self.ball_p[1], self.ball_p[2]),
                velocity=Vector3(self.ball_v[0], self.ball_v[1], self.ball_v[2]),
                angular_velocity=zero)),
            cars={
                0: CarState(
                    physics=Physics(
                        location=Vector3(self.test_car_p[0], self.test_car_p[1], car_at_rest),
                        rotation=Rotator(0, self.test_car_yaw, 0),
                        velocity=zero,
                        angular_velocity=zero),
                    boost_amount=100),
                1: CarState(
                    physics=Physics(
                        location=Vector3(self.car_1_p[0], self.car_1_p[1], car_at_rest),
                        rotation=Rotator(0, self.car_1_yaw, 0),
                        velocity=zero,
                        angular_velocity=zero),
                    boost_amount=100),
                2: CarState(
                    physics=Physics(
                        location=Vector3(self.car_2_p[0], self.car_2_p[1], car_at_rest),
                        rotation=Rotator(0, self.car_2_yaw, 0),
                        velocity=zero,
                        angular_velocity=zero),
                    boost_amount=100),
                3: CarState(
                    physics=Physics(
                        location=Vector3(self.car_3_p[0], self.car_3_p[1], car_at_rest),
                        rotation=Rotator(0, self.car_3_yaw, 0),
                        velocity=zero,
                        angular_velocity=zero),
                    boost_amount=100),
                4: CarState(
                    physics=Physics(
                        location=Vector3(self.car_4_p[0], self.car_4_p[1], car_at_rest),
                        rotation=Rotator(0, self.car_4_yaw, 0),
                        velocity=zero,
                        angular_velocity=zero),
                    boost_amount=100),
                5: CarState(
                    physics=Physics(
                        location=Vector3(self.car_5_p[0], self.car_5_p[1], car_at_rest),
                        rotation=Rotator(0, self.car_5_yaw, 0),
                        velocity=zero,
                        angular_velocity=zero),
                    boost_amount=100),
            },
        )
