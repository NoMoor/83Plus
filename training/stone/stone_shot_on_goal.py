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
    ball_start_x: float = 0
    ball_start_y: float = 4000
    car_start_x: float = 0
    car_start_y: float = -5000

    def make_game_state(self, rng: SeededRandomNumberGenerator) -> GameState:
        return GameState(
            ball=BallState(physics=Physics(
                location=Vector3(self.ball_start_x, self.ball_start_y, 100),
                velocity=Vector3(0, 0, 0),
                angular_velocity=Vector3(0, 0, 0))),
            cars={
                0: CarState(
                    physics=Physics(
                        location=Vector3(self.car_start_x, self.car_start_y, 0),
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
        # StoneShotOnGoal('Straight', ball_start_x=0, ball_start_y=3500, car_start_x=0, car_start_y=0),
        # StoneShotOnGoal('Close Angle Left 5', ball_start_x=500, ball_start_y=3500, car_start_x=0, car_start_y=0),
        # StoneShotOnGoal('Close Angle Right 5', ball_start_x=-500, ball_start_y=3500, car_start_x=0, car_start_y=0),
        # StoneShotOnGoal('Close Angle Left 6', ball_start_x=600, ball_start_y=3500, car_start_x=0, car_start_y=0),
        # StoneShotOnGoal('Close Angle Right 6', ball_start_x=-600, ball_start_y=3500, car_start_x=0, car_start_y=0),
        # StoneShotOnGoal('Close Angle Left 8', ball_start_x=800, ball_start_y=3500, car_start_x=0, car_start_y=0),
        # StoneShotOnGoal('Close Angle Right 8', ball_start_x=-800, ball_start_y=3500, car_start_x=0, car_start_y=0),
        # StoneShotOnGoal('Close Angle Left 10', ball_start_x=1000, ball_start_y=3500, car_start_x=0, car_start_y=0),
        # StoneShotOnGoal('Close Angle Right 10', ball_start_x=-1000, ball_start_y=3500, car_start_x=0, car_start_y=0),
        # StoneShotOnGoal('Close Angle Left 12', ball_start_x=1200, ball_start_y=3500, car_start_x=0, car_start_y=0),
        # StoneShotOnGoal('Close Angle Right 12', ball_start_x=-1200, ball_start_y=3500, car_start_x=0, car_start_y=0),
        # StoneShotOnGoal('Close Angle Left 14', ball_start_x=1400, ball_start_y=3500, car_start_x=0, car_start_y=0),
        # StoneShotOnGoal('Close Angle Right 14', ball_start_x=-1400, ball_start_y=3500, car_start_x=0, car_start_y=0),
        # StoneShotOnGoal('Close Angle Left 16', ball_start_x=1600, ball_start_y=3500, car_start_x=0, car_start_y=0),
        # StoneShotOnGoal('Close Angle Right 16', ball_start_x=-1600, ball_start_y=3500, car_start_x=0, car_start_y=0),

        StoneShotOnGoal('Med Angle Left 14', ball_start_x=1400, ball_start_y=2000, car_start_x=0, car_start_y=-1000),
        StoneShotOnGoal('Med Angle Right 14', ball_start_x=-1400, ball_start_y=2000, car_start_x=0, car_start_y=-1000),

        StoneShotOnGoal('Long Angle Left 14', ball_start_x=1400, ball_start_y=0, car_start_x=0, car_start_y=-3000),
        StoneShotOnGoal('Long Angle Right 14', ball_start_x=-1400, ball_start_y=0, car_start_x=0, car_start_y=-3000),
    ]
