from dataclasses import dataclass
from math import pi

from rlbot.utils.game_state_util import GameState, BoostState, BallState, CarState, Physics, Vector3, Rotator

from rlbottraining.common_exercises.common_base_exercises import StrikerExercise, GoalieExercise
from rlbottraining.rng import SeededRandomNumberGenerator
from rlbottraining.training_exercise import Playlist


@dataclass
class StoneShotOnGoal(GoalieExercise):

    def make_game_state(self, rng: SeededRandomNumberGenerator) -> GameState:
        return GameState(
            ball=BallState(physics=Physics(
                location=Vector3(rng.uniform(-1000, 1000), rng.uniform(-2000, 2000), 0),
                velocity=Vector3(0, 0, 0),
                angular_velocity=Vector3(0, 0, 0))),
            cars={
                0: CarState(
                    physics=Physics(
                        location=Vector3(rng.uniform(-800, 800), -5800, 0),
                        rotation=Rotator(0, pi / 2, 0),
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
