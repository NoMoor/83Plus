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
class KickOff(TrainingExercise):
    grader: Grader = field(default_factory=lambda: GameTickPacketWrapperGrader(
        StrikerGrader(timeout_seconds=5.0, ally_team=0)))
    car_start_x: float = 0
    car_start_y: float = -4608
    car_yaw: float = 0

    def make_game_state(self, rng: SeededRandomNumberGenerator) -> GameState:
        return GameState(
            ball=BallState(physics=Physics(
                location=Vector3(0, 0, 100),
                velocity=Vector3(0, 0, 0),
                angular_velocity=Vector3(0, 0, 0))),
            cars={
                0: CarState(
                    physics=Physics(
                        location=Vector3(self.car_start_x, self.car_start_y, 16.5),
                        rotation=Rotator(0, self.car_yaw, 0),
                        velocity=Vector3(0, 0, 0),
                        angular_velocity=Vector3(0, 0, 0)),
                    boost_amount=33)
            },
            boosts={7: BoostState(100), 13: BoostState(100)},
        )

def make_default_playlist() -> Playlist:
    return [
        KickOff('Center Kickoff', car_start_x=0, car_start_y=-4608, car_yaw=(.5 * pi)),
        KickOff('Left Kickoff', car_start_x=2048, car_start_y=-2560, car_yaw=(.75 * pi)),
        KickOff('Left Center Kickoff', car_start_x=256, car_start_y=-3840, car_yaw=(.5 * pi)),
        KickOff('Right Center Kickoff', car_start_x=-256, car_start_y=-3840, car_yaw=(.5 * pi)),
        KickOff('Right Kickoff', car_start_x=-2048, car_start_y=-2560, car_yaw=(-.25 * pi)),
    ]
