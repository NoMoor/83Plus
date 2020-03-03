from dataclasses import dataclass, field
from math import pi

from rlbot.utils.game_state_util import GameState, BoostState, BallState, CarState, Physics, Vector3, Rotator
from rlbottraining.common_exercises.common_base_exercises import StrikerExercise
from rlbottraining.common_graders.goal_grader import StrikerGrader
from rlbottraining.grading.grader import Grader
from rlbottraining.rng import SeededRandomNumberGenerator
from rlbottraining.training_exercise import Playlist, TrainingExercise


def make_grader():
    # return StrikerGrader(timeout_seconds=6.0)
    return StrikerGrader(timeout_seconds=4.0)

@dataclass
class StoneShotOnGoal(TrainingExercise):
    grader: Grader = field(default_factory=make_grader)
    ball_x: float = 0
    ball_y: float = 4000
    ball_z: float = 100
    ball_vx: float = 0
    ball_vy: float = 0
    ball_vz: float = 0
    car_x: float = 0
    car_y: float = -5000
    car_spin: float = pi / 2

    def make_game_state(self, rng: SeededRandomNumberGenerator) -> GameState:
        return GameState(
            ball=BallState(physics=Physics(
                # location=Vector3(self.ball_x + rng.uniform(-10, 10), self.ball_y + rng.uniform(-10, 10), self.ball_z),
                location=Vector3(self.ball_x + rng.uniform(-500, 500), self.ball_y + rng.uniform(-500, 500),
                                 self.ball_z),
                velocity=Vector3(self.ball_vx + rng.uniform(-500, 500), self.ball_vy + rng.uniform(-500, 500),
                                 self.ball_vz),
                angular_velocity=Vector3(0, 0, 0))),
            cars={
                0: CarState(
                    physics=Physics(
                        # location=Vector3(self.car_start_x + rng.uniform(-1500, 1500), self.car_start_y, 0),
                        location=Vector3(self.car_x, self.car_y, 17),
                        rotation=Rotator(0, self.car_spin, 0),
                        velocity=Vector3(0, 0, 0),
                        angular_velocity=Vector3(0, 0, 0)),
                    jumped=False,
                    double_jumped=False,
                    boost_amount=100)
            },
            boosts={i: BoostState(0) for i in range(34)}, # Is this needed.
        )

@dataclass
class RollingTowardsGoalShot(StrikerExercise):
    def make_game_state(self, rng: SeededRandomNumberGenerator) -> GameState:
        return GameState(
            ball=BallState(physics=Physics(
                # location=Vector3(1000 * rng.n11(), rng.uniform(0, 1500), 100),
                location=Vector3(400, 1100, 100),
                velocity=Vector3(0, 550, 0),
                angular_velocity=Vector3(0, 0, 0))),
            cars={
                0: CarState(
                    physics=Physics(
                        location=Vector3(0, -2500, 18),
                        rotation=Rotator(0, pi / 2, 0),
                        velocity=Vector3(0, 0, 0),
                        angular_velocity=Vector3(0, 0, 0)),
                    boost_amount=87),
            },
        )

@dataclass
class RollingAcross(StrikerExercise):
    def make_game_state(self, rng: SeededRandomNumberGenerator) -> GameState:
        return GameState(
            ball=BallState(physics=Physics(
                # location=Vector3(1000 * rng.n11(), rng.uniform(0, 1500), 100),
                location=Vector3(rng.uniform(-1000, 1000), rng.uniform(-1000, 1000), 100),
                velocity=Vector3(rng.uniform(-1000, 1000), rng.uniform(-1000, 1000), 1000),
                angular_velocity=Vector3(0, 0, 0))),
            cars={
                0: CarState(
                    physics=Physics(
                        location=Vector3(0, -2500, 18),
                        rotation=Rotator(0, pi / 2, 0),
                        velocity=Vector3(0, 0, 0),
                        angular_velocity=Vector3(0, 0, 0)),
                    boost_amount=87),
            },
        )


@dataclass
class BallDrop(TrainingExercise):
    grader: Grader = field(default_factory=make_grader)
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
    car_y: float = 10
    car_z: float = 17
    car_spin: float = 0
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
                        rotation=Rotator(self.car_pitch, self.car_spin, self.car_roll),
                        velocity=Vector3(0, 0, 0),
                        angular_velocity=Vector3(0, 0, 0)),
                    boost_amount=100)
            },
            boosts={i: BoostState(0) for i in range(34)},  # Is this needed.
        )


def make_default_playlist() -> Playlist:
    return [
        RollingAcross("Rolling across"),
        # RollingTowardsGoalShot("Rolling shot"),
        # StoneShotOnGoal('Straight', ball_x=0, ball_y=3500, car_start_x=0, car_start_y=0),
        # StoneShotOnGoal('Close Angle Left 5', ball_x=500, ball_y=3500, car_start_x=0, car_start_y=0),
        # StoneShotOnGoal('Close Angle Right 5', ball_x=-500, ball_y=3500, car_start_x=0, car_start_y=0),
        # StoneShotOnGoal('Close Angle Left 6', ball_x=600, ball_y=3500, car_start_x=0, car_start_y=0),
        # StoneShotOnGoal('Close Angle Right 6', ball_x=-600, ball_y=3500, car_start_x=0, car_start_y=0),
        # StoneShotOnGoal('Close Angle Left 8', ball_x=800, ball_y=3500, car_start_x=0, car_start_y=0),
        # StoneShotOnGoal('Close Angle Right 8', ball_x=-800, ball_y=3500, car_start_x=0, car_start_y=0),
        # StoneShotOnGoal('Close Angle Left 10', ball_x=1000, ball_y=3500, car_start_x=0, car_start_y=0),
        # StoneShotOnGoal('Close Angle Right 10', ball_x=-1000, ball_y=3500, car_start_x=0, car_start_y=0),
        # StoneShotOnGoal('Close Angle Left 12', ball_x=1200, ball_y=3500, car_start_x=0, car_start_y=0),
        # StoneShotOnGoal('Close Angle Right 12', ball_x=-1200, ball_y=3500, car_start_x=0, car_start_y=0),
        # StoneShotOnGoal('Close Angle Left 14', ball_x=1400, ball_y=3500, car_start_x=0, car_start_y=0),
        # StoneShotOnGoal('Close Angle Right 14', ball_x=-1400, ball_y=3500, car_start_x=0, car_start_y=0),
        # StoneShotOnGoal('Close Angle Left 16', ball_x=1600, ball_y=3500, car_start_x=0, car_start_y=0),
        # StoneShotOnGoal('Close Angle Right 16', ball_x=-1600, ball_y=3500, car_start_x=0, car_start_y=0),
        #
        StoneShotOnGoal('Med Angle Left 14', ball_x=1400, ball_y=2000, car_start_y=-1000),
        StoneShotOnGoal('Med Angle Right 14', ball_x=-1400, ball_y=2000, car_start_x=0, car_start_y=-1000),
        #
        StoneShotOnGoal('Long Angle Left 14', ball_x=1400, ball_y=0, car_start_x=0, car_start_y=-3000),
        StoneShotOnGoal('Long Angle Right 14', ball_x=-1400, ball_y=0, car_start_x=0, car_start_y=-3000),
    ]
