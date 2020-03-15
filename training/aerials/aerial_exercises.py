from aerials.aerial_playground_training import PlayGround

# playground.match_config.mutators.boost_amount = "Unlimited"

aerial_exercises = [
    PlayGround('Toss Up', ball_x=500, ball_z=1000, ball_vz=1000),
    PlayGround('Bounce', ball_x=-500, ball_z=1000, ball_vz=-2000),
    PlayGround('Lateral L2R', ball_x=2000, ball_y=0, ball_z=1000, ball_vx=-500, ball_vz=1000),
    PlayGround('Ball Drop', ball_x=0, ball_y=1000, ball_z=1250, ball_vz=100),
    # PlayGround('Aerial Training', ball_x=0, ball_y=0, ball_z=1000, ball_vz=-2000),
    # PlayGround('Aerial Training', ball_x=3800, ball_y=0, ball_z=1000, ball_vx=-2000, ball_vz=-2000),
]
