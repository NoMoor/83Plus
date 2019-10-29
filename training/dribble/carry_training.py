from dataclasses import dataclass, field
from math import pi

from rlbot.utils.game_state_util import GameState, BoostState, BallState, CarState, Physics, Vector3, Rotator

from rlbottraining.training_exercise import TrainingExercise
from rlbottraining.common_graders.goal_grader import StrikerGrader
from rlbottraining.common_graders.tick_wrapper import GameTickPacketWrapperGrader
from rlbottraining.grading.grader import Grader
from rlbottraining.rng import SeededRandomNumberGenerator
from rlbottraining.training_exercise import Playlist


@dataclass
class StationaryBall(TrainingExercise):
    grader: Grader = field(default_factory=lambda: GameTickPacketWrapperGrader(
        StrikerGrader(timeout_seconds=8.0, ally_team=0)))
    ball_start_x: float = 0
    ball_start_y: float = -3000
    ball_start_z: float = 120
    car_start_x: float = 1000
    car_start_y: float = -4000
    car_angle: float = pi / 2


    def make_game_state(self, rng: SeededRandomNumberGenerator) -> GameState:
        return GameState(
            ball=BallState(physics=Physics(
                location=Vector3(self.ball_start_x + rng.uniform(-30, 30), self.ball_start_y, self.ball_start_z),
                velocity=Vector3(0, 0, 0),
                angular_velocity=Vector3(0, 0, 0))),
            cars={
                0: CarState(
                    physics=Physics(
                        location=Vector3(self.car_start_x, self.car_start_y, 0),
                        rotation=Rotator(0, self.car_angle, 0),
                        velocity=Vector3(0, 0, 0),
                        angular_velocity=Vector3(0, 0, 0)),
                    jumped=False,
                    double_jumped=False,
                    boost_amount=100)
            },
            boosts={1: BoostState(100)},
        )


def make_default_playlist() -> Playlist:
    return [
        StationaryBall('BallOnGround', ball_start_z=90, car_start_x=0, car_start_y=-1000),
    ]
