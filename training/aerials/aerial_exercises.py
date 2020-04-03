from aerials.aerial_playground_training import Setup

# playground.match_config.mutators.boost_amount = "Unlimited"

aerial_exercises = [
    # Setup('Toss Up', ball_x=500, ball_y=2000, ball_z=1000, ball_vz=1000),
    # Setup('Jump Ball', ball_z=100, ball_vz=1500),
    # Setup('Jump Ball', ball_z=500, ball_vz=-2500),
    # Setup('Lateral L2R', ball_x=3000, ball_z=1000, ball_vx=-1200, ball_vz=1000, minboost=20, maxboost=50),
    # Setup('Redirect from mid-field', ball_x=3000, ball_y=-3000, ball_z=1000, ball_vx=-1200, ball_vz=1000, ball_vy=2000,
    #       boost=100, car_y=0, car_x=-3000),
    Setup('Redirect from far-corner', ball_x=3000, ball_y=-3000, ball_z=1000, ball_vx=-1200, ball_vz=1000, ball_vy=2000,
          boost=100, car_y=2000, car_x=-3000),
    # Setup('Bounce', ball_x=-500, ball_y=2000, ball_z=1000, ball_vz=-2000),
    # Setup('Lateral L2R', ball_x=2000, ball_z=1000, ball_vx=-500, ball_vz=1000),
    # Setup('Ball Drop', ball_x=0, ball_y=1000, ball_z=1250, ball_vz=100),
    # Setup('Aerial Training', ball_x=0, ball_z=1000, ball_vz=-2000),
    # Setup('Aerial Training', ball_x=3800, ball_z=1000, ball_vx=-2000, ball_vz=-2000),
]
