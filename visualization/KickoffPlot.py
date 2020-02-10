# This import registers the 3D projection, but is otherwise unused.

import matplotlib.pyplot as plt
from google.protobuf import json_format

from visualization.gamestate_pb2 import GameState


def read_file():
    f = open("data/state_logger_4.log", "rt")

    game_states = []

    for line in f.readlines():
        game_states.append(json_format.Parse(line, GameState()))

    f.close()
    return game_states


def plot_ball(attempt):
    xs = []
    ys = []
    zs = []
    scores = False
    for state in attempt:
        # Stop plotting after the ball has been hit.
        if not ball_is_at_center(state):
            xs.append(state.ball.pos[0])
            ys.append(state.ball.pos[1])
            zs.append(state.ball.pos[2])
        if ball_is_in_goal(state):
            scores = True

    ax.plot(xs, ys, zs, ",-k" if scores else ",-w")


def make_3d_graph():
    scoring_attempts = []
    nonscoring_attempts = []
    whiff_attempts = []

    for attempt in game_state_map.values():
        if misses_ball(attempt):
            whiff_attempts.append(attempt)
        elif scores_goal(attempt):
            scoring_attempts.append(attempt)
        else:
            nonscoring_attempts.append(attempt)

    for attempt in scoring_attempts:
        plot_attempt(attempt, ",-g", "scoring")

    for attempt in nonscoring_attempts:
        plot_attempt(attempt, ",-y", "nonscoring")

    for attempt in whiff_attempts:
        plot_attempt(attempt, ",-r", "whiff")

    for attempt in game_state_map.values():
        plot_ball(attempt)

    print(f'Summary: Total {len(game_state_map.values())} scoring: {len(scoring_attempts)} non-scoring: \
          {len(nonscoring_attempts)} whiff: {len(whiff_attempts)}')


def plot_attempt(attempt, fmt, label):
    if not attempt:
        return False

    xs = []
    ys = []
    zs = []
    for state in attempt:
        # Stop plotting after the ball has been hit.
        if car_state_is_not_relevant(state):
            break
        xs.append(state.car[0].pos[0])
        ys.append(state.car[0].pos[1])
        zs.append(state.car[0].pos[2])

    ax.plot(xs, ys, zs, fmt, label=label + str(attempt[0].training_id))
    return True


def car_state_is_not_relevant(game_state):
    return game_state.ball.pos[1] > 200 or abs(game_state.ball.pos[0]) > 200


def misses_ball(game_states):
    for state in game_states:
        if not ball_is_at_center(state):
            return False
    return True


def scores_goal(game_states):
    for state in game_states:
        if ball_is_in_goal(state):
            return True
    return False


def ball_is_at_center(state):
    if state.ball.pos[1] == 0 and state.ball.pos[0] == 0:
        return True
    return False


def ball_is_in_goal(state):
    if state.ball.pos[0] == 0:
        return False
    if abs(state.ball.pos[1]) > 4000 and abs(state.ball.pos[0]) < 600:
        return True

    return False


def plot_telemetry(attempt, fmt):
    vx = []
    vy = []
    vz = []
    sx = []
    sy = []
    sz = []
    for frame in attempt:
        if car_state_is_not_relevant(frame):
            break
        vx.append(frame.car[0].vel[0])
        vy.append(frame.car[0].vel[1])
        vz.append(frame.car[0].vel[2])
        sx.append(frame.car[0].spin[0])
        sy.append(frame.car[0].spin[1])
        sz.append(frame.car[0].spin[2])

    x = list(range(0, len(vx)))
    axs[0].plot(x, vx, fmt)
    axs[1].plot(x, vy, fmt)
    axs[2].plot(x, vz, fmt)
    axs[3].plot(x, sx, fmt)
    axs[4].plot(x, sy, fmt)
    axs[5].plot(x, sz, fmt)


def make_telemetry_graphs():
    scoring_attempts = []
    nonscoring_attempts = []
    whiff_attempts = []

    for attempt in game_state_map.values():
        if misses_ball(attempt):
            whiff_attempts.append(attempt)
        elif scores_goal(attempt):
            scoring_attempts.append(attempt)
        else:
            nonscoring_attempts.append(attempt)

    val = 0
    for attempt in nonscoring_attempts:
        plot_telemetry(attempt, 'tab:orange')
        val += 1
        if val > 5:
            break

    val = 0
    for attempt in whiff_attempts:
        plot_telemetry(attempt, 'tab:red')
        val += 1
        if val > 5:
            break

    val = 0
    for attempt in scoring_attempts:
        plot_telemetry(attempt, 'tab:green')
        val += 1
        if val > 5:
            break


def make_map(states):
    ret_value = {}
    for state in states:
        if state.training_id not in ret_value:
            ret_value[state.training_id] = []
        ret_value[state.training_id].append(state)
    return ret_value


fig = plt.figure()
ax = fig.gca(projection='3d')

game_state_map = make_map(read_file())
make_3d_graph()

# Put a ball in the center
ax.scatter([0], [0], [92], color='.75', s=93)

ax.set_xlim(4096, -4096)
ax.set_ylim(-5120, 5120)
ax.set_zlim(0, 2000)
ax.set_xlabel('X')
ax.set_ylabel('Y')
ax.set_zlabel('Z')
ax.set_title('Speed Flip Results from left and right kickoff locations')
ax.legend()

fig2, axs = plt.subplots(6, sharex=True)
make_telemetry_graphs()
fig2.suptitle('Telemetry')
axs[0].set_title('Velocity X')
axs[0].set(ylabel='uu')
axs[1].set_title('Velocity Y')
axs[1].set(ylabel='uu')
axs[2].set_title('Velocity Z')
axs[2].set(ylabel='uu')
axs[3].set_title('Rotation X')
axs[3].set(ylabel='radians')
axs[4].set_title('Rotation Y')
axs[4].set(ylabel='radians')
axs[5].set_title('Rotation Z')
axs[5].set(ylabel='radians')

plt.show()
