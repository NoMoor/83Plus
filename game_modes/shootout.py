import time
from math import pi
from rlbot.setup_manager import SetupManager
from rlbot.utils.game_state_util import GameState, CarState, Physics, Vector3, Rotator, BallState
from rlbot.utils.structures.game_data_struct import GameTickPacket


# Constants for this game mode
class ShootOut:
    zero = Vector3(x=0, y=0, z=0)

    facing_orange = Rotator(pitch=0, yaw=pi / 2, roll=0)
    orange_goal_line = Vector3(x=0, y=5120, z=20)
    orange_pk = Vector3(x=0, y=1000, z=100)

    facing_blue = Rotator(pitch=0, yaw=-pi / 2, roll=0)
    blue_goal_line = Vector3(x=0, y=-5120, z=20)
    blue_pk = Vector3(x=0, y=-1000, z=100)

    fps = 120
    game_speed = 1

    blue_score = -1
    orange_score = -1

    def __init__(self):
        self.last_reset_time = 0
        self.defender_team = 0
        self.player_index = 0
        self.ball_reset_time = 0

    def players_on_team(self, packet, team_id):
        return sum(packet.game_cars[i].team == team_id for i in range(packet.num_cars))

    def attacker(self, packet):
        player_index = self.player_index % self.players_on_team(packet, (self.defender_team + 1) % 2)
        index = 0
        for i in range(packet.num_cars):
            car = packet.game_cars[i]
            if car.team == self.defender_team:
                continue
            if player_index == index:
                return i
            index += 1

    def defender(self, packet):
        """Always return the first player on the team."""
        for i in range(packet.num_cars):
            car = packet.game_cars[i]
            if car.team == self.defender_team:
                return i

    def index_on_team(self, packet, car):
        team_index = car.team
        count = 0
        for i in range(packet.num_cars):
            next = packet.game_cars[i]
            if next.team != team_index:
                continue
            if car == next:
                break
            count += 1
        return count

    def freeze_team(self, packet, cars):
        attacker_index = self.attacker(packet)
        defender_index = self.defender(packet)

        for i in range(packet.num_cars):
            is_attacker = i == attacker_index
            is_defender = i == defender_index
            if is_attacker or is_defender:
                continue

            car = packet.game_cars[i]
            car_state = CarState()

            is_blue_team = car.team == 0
            team_modifier = -1 if is_blue_team else 1

            location = Vector3(
                x=3500,
                y=team_modifier * (400 + (self.index_on_team(packet, car) * 100)),
                z=20)
            velocity = self.zero
            rotation = Rotator(pitch=0, yaw=pi, roll=0)

            car_state.physics = Physics(location=location,
                                        velocity=velocity,
                                        angular_velocity=self.zero,
                                        rotation=rotation)
            car_state.boost_amount = 0
            cars[i] = car_state

    def setup_showdown(self, packet, cars):
        """Sets the attacker and defender for this shot"""
        attacker_index = self.attacker(packet)
        defender_index = self.defender(packet)

        for i in range(packet.num_cars):
            is_attacker = i == attacker_index
            is_defender = i == defender_index
            if not (is_attacker or is_defender):
                continue

            car = packet.game_cars[i]
            car_state = CarState()

            is_blue_team = car.team == 0
            team_modifier = -1 if is_blue_team else 1
            location = Vector3(0, team_modifier * 2000, 20) if i == attacker_index else \
                (self.blue_goal_line if is_blue_team else self.orange_goal_line)
            rotation = self.facing_orange if is_blue_team else self.facing_blue

            car_state.physics = Physics(location=location,
                                        velocity=self.zero,
                                        angular_velocity=self.zero,
                                        rotation=rotation)
            car_state.boost_amount = 30 if is_attacker else 12
            cars[i] = car_state

    def reset_ball(self, packet, game_state):
        self.ball_reset_time = packet.game_info.seconds_elapsed
        attacking_team_offset = -1 if self.defender_team == 0 else 1
        game_state.ball = BallState(Physics(location=Vector3(0, attacking_team_offset * 500, 120), velocity=self.zero))

    def make_game_state(self, packet):
        game_state = GameState()

        cars = {}
        # If the score is different, update cycle to the next cars
        if (packet.teams[0].score != self.blue_score or packet.teams[1].score != self.orange_score) \
                and self.is_kickoff(packet):
            self.next_players(packet)
            self.setup_showdown(packet, cars)
            self.reset_ball(packet, game_state)
        elif self.should_judge_score(packet):
            self.render_judgement(packet, game_state)

        # Ensure the other cars do not move
        self.freeze_team(packet, cars)
        if self.last_reset_time < packet.game_info.seconds_elapsed - 3:
            self.last_reset_time = packet.game_info.seconds_elapsed

        game_state.cars = cars
        return game_state

    def should_judge_score(self, packet):
        return self.ball_reset_time + 5 < packet.game_info.seconds_elapsed

    def next_players(self, packet):
        self.blue_score = packet.teams[0].score
        self.orange_score = packet.teams[1].score
        goal_total = self.blue_score + self.orange_score
        self.player_index += goal_total // 2
        self.defender_team = (self.defender_team + 1) % 2

    def render_judgement(self, packet, game_state):
        defending_team_modifier = 1 if self.defender_team == 0 else -1
        ballLocation = packet.game_ball.physics.location
        if abs(ballLocation.x) < 10 and ((defending_team_modifier > 0 and ballLocation.y > 4900)
                                         or (defending_team_modifier < 0 and ballLocation.y < -4900)):
            return

        game_state.ball = BallState(physics=Physics(
            location=Vector3(x=0, y=defending_team_modifier * 5000, z=100),
            velocity=Vector3(x=0, y=defending_team_modifier * 500, z=100)))

    def is_kickoff(self, packet):
        ball_velocity = packet.game_ball.physics.velocity
        ball_location = packet.game_ball.physics.location
        return ball_location.x < 1 and ball_location.y < 1 and ball_velocity.x < 1 and ball_velocity.y < 1


class Observer:
    def __init__(self):
        self.manager = SetupManager()
        self.manager.connect_to_game()
        self.game_interface = self.manager.game_interface
        self.main()

    def main(self):
        shootout = ShootOut()

        # TODO: Fail if the game doesn't have at least 2 cars

        # state setting
        while True:
            # updating packet
            packet = GameTickPacket()
            self.game_interface.update_live_data_packet(packet)

            if packet.num_cars < 1 or not packet.game_info.is_round_active:
                continue

            self.game_interface.set_game_state(shootout.make_game_state(packet))

            time.sleep(1 / (shootout.fps * shootout.game_speed))


if __name__ == "__main__":
    obv = Observer()
