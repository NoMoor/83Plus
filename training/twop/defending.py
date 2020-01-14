from dataclasses import dataclass, field
from math import pi

from rlbot.utils.game_state_util import GameState, BoostState, BallState, CarState, Physics, Vector3, Rotator
from rlbottraining.common_graders.goal_grader import GoalieGrader
from rlbottraining.grading.grader import Grader
from rlbottraining.rng import SeededRandomNumberGenerator
from rlbottraining.training_exercise import Playlist, TrainingExercise


def make_grader():
    return GoalieGrader(timeout_seconds=10.0)


@dataclass
class Defending(TrainingExercise):
    grader: Grader = field(default_factory=make_grader)
    ball_x: float = 0
    ball_y: float = 10
    ball_z: float = 150
    ball_vx: float = 0
    ball_vy: float = 0
    ball_vz: float = 0
    ball_sx: float = 0
    ball_sy: float = 0
    ball_sz: float = 0.1

    car_x: float = 0
    car_y: float = 10
    car_z: float = 17
    car_spin: float = pi / 2

    def make_game_state(self, rng: SeededRandomNumberGenerator) -> GameState:
        return GameState(
            ball=BallState(physics=Physics(
                location=Vector3(self.ball_x, self.ball_y, self.ball_z),
                velocity=Vector3(self.ball_vx, self.ball_vy, self.ball_vz),
                angular_velocity=Vector3(self.ball_sx, self.ball_sy, self.ball_sz))),
            cars={
                0: CarState(
                    physics=Physics(
                        location=Vector3(self.car_x, self.car_y, self.car_z),
                        rotation=Rotator(0, self.car_spin, 0),
                        velocity=Vector3(0, 0, 0),
                        angular_velocity=Vector3(0, 0, 0)),
                    jumped=False,
                    double_jumped=False,
                    boost_amount=100),
                1: CarState(
                    physics=Physics(
                        location=Vector3(self.car_x, -self.car_y, self.car_z),
                        rotation=Rotator(0, -self.car_spin, 0),
                        velocity=Vector3(0, 0, 0),
                        angular_velocity=Vector3(0, 0, 0)),
                    jumped=False,
                    double_jumped=False,
                    boost_amount=100)
            },
            boosts={i: BoostState(0) for i in range(34)},  # Is this needed.
        )


def make_default_playlist() -> Playlist:
    return [
    ]
