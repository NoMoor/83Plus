from dataclasses import dataclass, field
from math import pi

from rlbot.utils.game_state_util import GameState, BoostState, BallState, CarState, Physics, Vector3, Rotator

from rlbottraining.training_exercise import TrainingExercise
from rlbottraining.common_graders.goal_grader import StrikerGrader
from rlbottraining.grading.grader import Grader
from rlbottraining.rng import SeededRandomNumberGenerator
from rlbottraining.common_graders.tick_wrapper import GameTickPacketWrapperGrader
from rlbottraining.training_exercise import Playlist

@dataclass
class FarAwayBall(TrainingExercise):
    grader: Grader = field(default_factory=lambda: GameTickPacketWrapperGrader(
        StrikerGrader(timeout_seconds=5.0, ally_team=0)))
    car_start_x: float = 1000
    car_start_y: float = -4608
    boost: float = 33

    def make_game_state(self, rng: SeededRandomNumberGenerator) -> GameState:
        return GameState(
            ball=BallState(physics=Physics(
                location=Vector3(0, 4500, 100),
                velocity=Vector3(0, 0, 0),
                angular_velocity=Vector3(0, 0, 0))),
            cars={
                0: CarState(
                    physics=Physics(
                        location=Vector3(self.car_start_x, self.car_start_y, 0),
                        rotation=Rotator(0, pi/2, 0),
                        velocity=Vector3(0, 0, 0),
                        angular_velocity=Vector3(0, 0, 0)),
                    jumped=False,
                    double_jumped=False,
                    boost_amount=self.boost)
            },
            boosts={1: BoostState(100)},
        )

def make_default_playlist() -> Playlist:
    return [
        FarAwayBall('Full Boost', boost=100),
        FarAwayBall('No Boost', boost=0),
    ]
