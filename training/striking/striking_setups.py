from dataclasses import dataclass, field
from math import pi
from rlbot.training.training import Pass, Grade
from rlbot.utils.game_state_util import GameState, BoostState, BallState, CarState, Physics, Vector3, Rotator
from rlbottraining.common_exercises.common_base_exercises import StrikerExercise
from rlbottraining.common_graders.compound_grader import CompoundGrader
from rlbottraining.common_graders.goal_grader import StrikerGrader
from rlbottraining.common_graders.timeout import FailOnTimeout
from rlbottraining.grading.grader import Grader
from rlbottraining.grading.training_tick_packet import TrainingTickPacket
from rlbottraining.rng import SeededRandomNumberGenerator
from rlbottraining.training_exercise import TrainingExercise
from typing import Optional, Union


@dataclass
class GroundNonMoving(StrikerExercise):
    ball_x: float = 0
    ball_y: float = 0
    ball_z: float = 100
    car_x: float = 0
    car_y: float = 0
    car_spin: float = pi / 2

    def make_game_state(self, rng: SeededRandomNumberGenerator) -> GameState:
        return GameState(
            ball=BallState(physics=Physics(
                location=Vector3(self.ball_x, self.ball_y, self.ball_z),
                velocity=Vector3(0, 0, 0),
                angular_velocity=Vector3(0, 0, 0))),
            cars={
                0: CarState(
                    physics=Physics(
                        location=Vector3(self.car_x, self.car_y, 17),
                        rotation=Rotator(0, self.car_spin, 0),
                        velocity=Vector3(0, 0, 0),
                        angular_velocity=Vector3(0, 0, 0)),
                    jumped=False,
                    double_jumped=False,
                    boost_amount=100)
            },
        )


@dataclass
class Strike(TrainingExercise):
    grader: Grader = field(default_factory=lambda: StrikerGrader(timeout_seconds=4))
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


@dataclass
class PowerSlide(TrainingExercise):
    grader: Grader = field(default_factory=lambda: SlideGrader())
    ball_y: float = 4500
    car_y: float = -3500
    car_spin: float = pi / 2

    def make_game_state(self, rng: SeededRandomNumberGenerator) -> GameState:
        return GameState(
            ball=BallState(physics=Physics(
                location=Vector3(0, self.ball_y, 100),
                velocity=Vector3(0, 0, 0),
                angular_velocity=Vector3(0, 0, 0))),
            cars={
                0: CarState(
                    physics=Physics(
                        location=Vector3(0, self.car_y, 17),
                        rotation=Rotator(0, self.car_spin, 0),
                        velocity=Vector3(0, 0, 0),
                        angular_velocity=Vector3(0, 0, 0)),
                    jumped=False,
                    double_jumped=False,
                    boost_amount=100)
            },
        )


@dataclass
class SlideGrader(CompoundGrader):

    def __init__(self, timeout_seconds=10, min_exercise_duration=4):
        super().__init__([
            PassOnSlideComplete(min_exercise_duration),
            FailOnTimeout(timeout_seconds)
        ])


@dataclass
class PassOnSlideComplete(Grader):
    min_exercise_duration: Union[int, float]
    initial_seconds_elapsed: type(None) = None
    car_slide_flag: bool = False

    class PassDueToCarSlide(Pass):
        def __repr__(self):
            return f'{super().__repr__()}: Car has stopped rotating.'

    def on_tick(self, tick: TrainingTickPacket) -> Optional[Grade]:

        packet = tick.game_tick_packet
        game_time = packet.game_info.seconds_elapsed
        car = packet.game_cars[0]

        if self.initial_seconds_elapsed is None:
            self.initial_seconds_elapsed = game_time

        slide_complete = abs(car.physics.angular_velocity.z) < .01 and (abs(car.physics.rotation.yaw) > (pi / 2) + .02
                                                                        or car.physics.location.y > 1000)
        min_exercise_duration_reached = game_time >= self.initial_seconds_elapsed + self.min_exercise_duration

        if slide_complete and not self.car_slide_flag:
            self.car_slide_flag = True

        if self.car_slide_flag and min_exercise_duration_reached:
            return self.PassDueToCarSlide()
        else:
            return None
