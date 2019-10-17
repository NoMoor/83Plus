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
class StationaryBall(TrainingExercise):

    grader: Grader = field(default_factory=lambda: GameTickPacketWrapperGrader(
        StrikerGrader(timeout_seconds=10.0, ally_team=0)))

    # Facing away from ball
    # def make_game_state(self, rng: SeededRandomNumberGenerator) -> GameState:
    #     return GameState(
    #         ball=BallState(physics=Physics(
    #             location=Vector3(0, -3000, 120),
    #             velocity=Vector3(0, 0, 0),
    #             angular_velocity=Vector3(0, 0, 0))),
    #         cars={
    #             0: CarState(
    #                 physics=Physics(
    #                     location=Vector3(1000, -4000, 0),
    #                     rotation=Rotator(0, -pi/4, 0),
    #                     velocity=Vector3(0, 0, 0),
    #                     angular_velocity=Vector3(0, 0, 0)),
    #                 jumped=False,
    #                 double_jumped=False,
    #                 boost_amount=100)
    #         },
    #         boosts={1: BoostState(100)},
    #     )

    def make_game_state(self, rng: SeededRandomNumberGenerator) -> GameState:
        return GameState(
            ball=BallState(physics=Physics(
                location=Vector3(0, -3000, 120),
                velocity=Vector3(0, 0, 0),
                angular_velocity=Vector3(0, 0, 0))),
            cars={
                0: CarState(
                    physics=Physics(
                        location=Vector3(1000, -4000, 0),
                        rotation=Rotator(0, 3 * pi/4, 0),
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
        StationaryBall('BallOnGround'),
    ]
