# This import registers the 3D projection, but is otherwise unused.
import re

import sys
from cycler import cycler
from google.protobuf import json_format
from matplotlib import pyplot as plt

from visualization.gamestate_pb2 import GameState


def color_list():
    cs = ['#00429d', '#06449e', '#0c469f', '#1147a0', '#1649a0', '#194ba1', '#1d4da2', '#204ea3', '#2350a4',
          '#2552a4', '#2854a5', '#2a56a6', '#2d57a7', '#2f59a7', '#315ba8', '#345da9', '#365fa9', '#3861aa',
          '#3a62ab', '#3c64ab', '#3e66ac', '#4068ad', '#426aad', '#446cae', '#466eaf', '#4870af', '#4a71b0',
          '#4c73b0', '#4e75b1', '#4f77b1', '#5179b2', '#537bb2', '#557db3', '#577fb3', '#5981b4', '#5a83b4',
          '#5c84b5', '#5e86b5', '#6088b5', '#628ab6', '#648cb6', '#658eb6', '#6790b7', '#6992b7', '#6b94b7',
          '#6d96b7', '#6f98b8', '#709ab8', '#729cb8', '#749eb8', '#76a0b8', '#78a2b8', '#7aa4b8', '#7ca6b8',
          '#7da7b8', '#7fa9b8', '#81abb8', '#83adb8', '#85afb8', '#87b1b8', '#89b3b7', '#8bb5b7', '#8db7b7',
          '#8fb9b7', '#91bbb6', '#93bdb6', '#95bfb5', '#97c1b5', '#99c3b4', '#9bc5b3', '#9dc7b3', '#9fc9b2',
          '#a2cbb1', '#a4cdb0', '#a6cfaf', '#a8d1ae', '#aad3ad', '#add5ac', '#afd7ab', '#b1d9a9', '#b4dba8',
          '#b6dda6', '#b9dfa4', '#bbe1a2', '#bde3a0', '#c0e59e', '#c3e79b', '#c5e999', '#c8eb96', '#cbed93',
          '#ceef8f', '#d1f18b', '#d4f387', '#d7f582', '#dbf77c', '#def975', '#e2fb6c', '#e6fc62', '#ebfe52',
          '#f3ff2c']
    cs.reverse()
    return cs


def read_file():
    f = open("data/drift/drift_then_boost.log", "rt")

    lines = []
    for line in f.readlines():
        lines.append(line)
    f.close()

    lines_parsed = 0
    game_states = []
    for line in lines:
        game_states.append(json_format.Parse(line, GameState()))
        lines_parsed += 1
        if lines_parsed % 10000 == 0:
            sys.stdout.write('\r' + f'Parsing: {100 * lines_parsed / len(lines):.0f}% complete')
            sys.stdout.flush()

    return game_states


def car_state_is_not_relevant(game_state):
    car = game_state.car[0]
    return (car.pos[1] < 0 and car.pos[0] < 100) or abs(car.spin[2]) < .01


def trainings(states):
    """Creates a dict from training_id to a list of game states."""
    ret_value = {}
    for state in states:
        if state.training_id not in ret_value:
            ret_value[state.training_id] = []
        ret_value[state.training_id].append(state)
    return ret_value


class Plotter:
    """Lists of 2 values for the min / max speed, steering angle, and hold ticks respectively. """

    def __init__(self, data, speeds=None, angles=None, holds=None, title=None, labeler=lambda i: str(i)):
        self.title = title
        self.labeler = labeler
        self.data = data
        if speeds is None:
            speeds = [0, 2300]
        self.speeds = speeds
        if angles is None:
            angles = [0, 1.0]
        self.angles = angles
        if holds is None:
            holds = [0, 100]
        self.holds = holds

    def filter_predicate(self):
        min_speed = self.speeds[0]
        max_speed = self.speeds[0] if len(self.speeds) == 1 else self.speeds[1]
        min_steer = self.angles[0]
        max_steer = self.angles[0] if len(self.angles) == 1 else self.angles[1]
        min_hold = self.holds[0]
        max_hold = self.holds[0] if len(self.holds) == 1 else self.holds[1]
        return lambda i: min_speed <= i.speed <= max_speed \
                         and min_steer <= i.steer <= max_steer \
                         and min_hold <= i.hold <= max_hold

    def plot_drifts(self, axis):
        axis.set_xlim([0, -1000])
        axis.set_ylim([0, 2000])
        axis.set_title(self.title)

        colors = color_list()
        axis.set_prop_cycle(cycler(color=colors))

        filtered_data = list(filter(self.filter_predicate(), self.data))

        for data in filtered_data:
            self._plot_drift(axis, data.frames, self.labeler(data))

        if len(filtered_data) < 50:
            axis.legend(loc='lower right')

    @staticmethod
    def _plot_drift(axis, attempt, label):
        if not attempt:
            return False

        xs = []
        ys = []
        for state in attempt:
            # Stop plotting after the ball has been hit.
            if car_state_is_not_relevant(state):
                continue
            xs.append(state.car[0].pos[0])
            ys.append(state.car[0].pos[1])

        axis.plot(xs, ys, label=label)

        # xs = []
        # ys = []
        # for state in attempt:
        #     # Stop plotting after the ball has been hit.
        #     if car_state_is_not_relevant(state) or not(is_straighten_out(state.label)):
        #         continue
        #     xs.append(state.car[0].pos[0])
        #     ys.append(state.car[0].pos[1])
        #
        # axis.plot(xs, ys, '-b')
        return True


def is_straighten_out(label):
    return label.startswith('straighten')


class DriftData:
    def __init__(self, descriptor, frames):
        values = re.findall(r"[-+]?\d*\.\d+|\d+", descriptor)
        self.speed = int(values[0])
        self.steer = float(values[1])
        self.hold = int(values[2])
        self.frames = frames

    def __str__(self):
        return f'speed: {self.speed} steering_angle: {self.steer} hold_count: {self.hold}'


class ChatLayout:
    rows = 1
    columns = 1
    chart_index = 0

    def next_chart(self):
        """Return the plot index to be used for the next chart"""
        self.chart_index += 1
        return self.chart_index + (self.rows * 100) + (self.columns * 10)


training_id_to_game_states = trainings(read_file())

drift_data = [DriftData(training_id_to_game_states[key][0].label, value)
              for key, value in training_id_to_game_states.items()]

layout = ChatLayout()

fig = plt.figure(figsize=(8, 12))
fig.suptitle('Drift Analysis by varying speed, steering angle and hold duration')
fig.text(.5,
         .95,
         'Drifts are notoriously complicated. The following plots attempt to isolate drift characteristics to \n'
         'show how the path changes with differences in speed, steering angle, and drift hold. All plots begin \n'
         'by driving at the specified speed, initiating a turn with the given steering angle and drift pressed \n'
         'and continue to hold this for the specified duration. After the initial hold, the drift is released. \n'
         'Each plot continues until spin (angular_velocity[2]) is sufficiently close to 0. Throttle is 1.0 always.',
         horizontalalignment='center',
         verticalalignment='top')

p1 = Plotter(drift_data,
             speeds=[0, 2300],
             holds=[40, 40],
             title=f'slide 40 angle 1.0 [variable speed]',
             labeler=lambda i: f'Speed {i.speed}')
# p1.plot_drifts(fig.add_subplot(layout.next_chart()))

p2 = Plotter(drift_data,
             speeds=[1980, 2020],
             title=f'speed ~2000 angle 1.0 [variable slide] w/ boost',
             labeler=lambda i: f'{i.hold} ticks')
p2.plot_drifts(fig.add_subplot(layout.next_chart()))

p3 = Plotter(drift_data,
             speeds=[1990, 2010],
             holds=[60, 60],
             angles=[0, 1.0],
             title=f'speed ~2000 slide 60 [variable angle]',
             labeler=lambda i: f'angle {i.steer}')
# p3.plot_drifts(fig.add_subplot(layout.next_chart()))

plt.tight_layout(rect=[0, 0, 1, .85])
plt.show()
