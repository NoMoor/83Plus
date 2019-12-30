from aerials.aerial_playground_training import PlayGround

# playground.match_config.mutators.boost_amount = "Unlimited"

aerial_exercises = [
    PlayGround('Aerial Training'),
    PlayGround('Aerial Training', ball_z=500),
    PlayGround('Aerial Training', ball_x=500, ball_z=750),
]