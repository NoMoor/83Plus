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
                location=Vector3(0, rng.uniform(-3000, 3000), rng.uniform(150, 280)),
                velocity=Vector3(0, 0, 0),
                angular_velocity=Vector3(0, 0, 0))),
            cars={
                0: CarState(
                    physics=Physics(
                        location=Vector3(rng.uniform(-200, 200), -5000, 0),
                        # location=Vector3(0, -5000, 0),
                        rotation=Rotator(0, pi / 2, 0),
                        velocity=Vector3(0, 0, 0),
                        angular_velocity=Vector3(0, 0, 0)),
                    jumped=False,
                    double_jumped=False,
                    boost_amount=100)
            },
            boosts={i: BoostState(0) for i in range(34)}, # Is this needed.
        )


def make_default_playlist() -> Playlist:
    return [
        StoneShotOnGoal('Shot on Goal'),
    ]
