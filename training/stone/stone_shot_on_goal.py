from dataclasses import dataclass, field
from math import pi

from rlbot.utils.game_state_util import GameState, BoostState, BallState, CarState, Physics, Vector3, Rotator

from rlbottraining.common_graders.goal_grader import StrikerGrader
from rlbottraining.rng import SeededRandomNumberGenerator
from rlbottraining.training_exercise import Playlist, TrainingExercise
from rlbottraining.grading.grader import Grader

def make_grader():
    return StrikerGrader(timeout_seconds=10.0)

@dataclass
class StoneShotOnGoal(TrainingExercise):
    grader: Grader = field(default_factory=make_grader)

    def make_game_state(self, rng: SeededRandomNumberGenerator) -> GameState:
        return GameState(
            ball=BallState(physics=Physics(
                location=Vector3(rng.uniform(-1000, 1000), rng.uniform(-2000, 2000), 100),
                # location=Vector3(100, 10, 100),
                velocity=Vector3(0, 0, 0),
                angular_velocity=Vector3(0, 0, 0))),
            cars={
                0: CarState(
                    physics=Physics(
                        # location=Vector3(rng.uniform(-800, 800), -5800, 0),
                        location=Vector3(0, 0, 1800),
                        rotation=Rotator(0, 1, 0),
                        velocity=Vector3(0, 0, 0),
                        angular_velocity=Vector3(0, 0, 0)),
                    jumped=False,
                    double_jumped=False,
                    boost_amount=0)
            },
            boosts={i: BoostState(0) for i in range(34)}, # Is this needed.
        )


def make_default_playlist() -> Playlist:
    return [
        StoneShotOnGoal('Shot on Goal'),
    ]
