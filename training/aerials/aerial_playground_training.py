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
class PlayGround(TrainingExercise):
    grader: Grader = field(default_factory=lambda: GameTickPacketWrapperGrader(
        StrikerGrader(timeout_seconds=20.0, ally_team=0)))

    def make_game_state(self, rng: SeededRandomNumberGenerator) -> GameState:
        return GameState(
            ball=BallState(physics=Physics(
                location=Vector3(0, 3000, 100),
                velocity=Vector3(0, 0, 0),
                angular_velocity=Vector3(0, 0, 0))),
            cars={
                0: CarState(
                    physics=Physics(
                        location=Vector3(0, 0, 20),
                        rotation=Rotator(pi / 2, 0, 0),
                        velocity=Vector3(0, 0, 0),
                        angular_velocity=Vector3(0, 0, 0)),
                    boost_amount=100)
            },
            boosts={i: BoostState(0) for i in range(34)},
        )

def make_default_playlist() -> Playlist:
    return [
        PlayGround('BallRollingToGoalie'),
    ]
