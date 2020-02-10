from dataclasses import dataclass, field

from rlbot.utils.game_state_util import GameState, BallState, CarState, Physics, Vector3, Rotator
from rlbottraining.common_graders.goal_grader import StrikerGrader
from rlbottraining.grading.grader import Grader
from rlbottraining.rng import SeededRandomNumberGenerator
from rlbottraining.training_exercise import TrainingExercise


@dataclass
class Rebound(TrainingExercise):
    grader: Grader = field(default_factory=lambda: StrikerGrader(timeout_seconds=6.0))
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
    car_y: float = 0
    car_z: float = 17
    car_vx: float = 0
    car_vy: float = 0
    car_vz: float = 17
    car_yaw: float = 0
    car_pitch: float = 0
    car_roll: float = 0

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
                        rotation=Rotator(self.car_pitch, self.car_yaw, self.car_roll),
                        velocity=Vector3(self.car_vx, self.car_vy, self.car_vz),
                        angular_velocity=Vector3(0, 0, 0)),
                    boost_amount=100)
            },
        )
