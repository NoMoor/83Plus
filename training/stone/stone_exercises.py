stone_exercises = [
    # Setup('Too Close for turning', ball_x=0, ball_y=100, car_x=120, car_y=-50),

    # Setup('Stationary centered mid-field', ball_x=0, ball_y=100, car_y=-2200),
    # Setup('Stationary left mid-field', ball_x=400, ball_y=100, car_y=-2200),
    # Setup('Stationary car very close to ball', ball_y=100, car_y=-200),
    # Setup('Rolling away left mid-field', ball_x=400, ball_y=100, ball_vy=400, car_y=-2200),
    # Setup('Rolling r2l mid-field', ball_x=-400, ball_vx=400, ball_y=100, car_y=-2200),
    # Setup('Fast rolling r2l mid-field', ball_x=-1500, ball_vx=1500, ball_y=100, car_y=-2200),

    # StoneKickoff('Stone Kickoff'),
    # StoneShotOnGoal('Stone Shot On Goal'),
    # RollingTowardsGoalShot("Rolling shot"),
    # RollingAcross("Rolling across"),

    # BallDrop("Flip testing", ball_x=0, ball_y=1750, ball_z=250, ball_sz=0, car_x=0, car_y=-500, car_spin=pi / 2),
    # BallDrop("Ceiling sticky testing", ball_x=0, ball_y=1750, ball_z=250, ball_sz=0, car_x=0, car_y=-500, car_z=2028, car_spin=-pi/2,
    #          car_pitch=pi),

    # Path planning ideas.
    # BallDrop("Path Testing", ball_x=1500, ball_y=1750, ball_z=100, ball_vz=0, ball_vx=0, car_y=-2000),
    # BallDrop("Path Testing", ball_x=1500, ball_y=1750, ball_z=100, ball_vz=0, ball_vx=0, car_x=-1000, car_y=2000),
    # BallDrop("Optimizer Testing", ball_x=1500, ball_y=1750, ball_z=100, ball_vz=0, ball_vx=0, car_x=2000, car_y=-2000,
    #          car_spin=0),
    # BallDrop("Path Testing Far away", ball_x=1500, ball_y=1750, ball_z=100, ball_vz=0, ball_vx=0, car_x=-1000, car_y=-2000,
    #              car_spin=0),
    # BallDrop("Path Close up", ball_x=1500, ball_y=1750, ball_z=100, ball_vz=0, ball_vx=0, car_x=1600, car_y=1000,
    #          car_spin=pi * 3 /4),
    # BallDrop("Path Testing", ball_x=0, ball_y=500, ball_z=100, ball_vz=1000, ball_vx=500, car_y=-2000),

    # Balls rolling toward and away from the center of the map
    # StoneShotOnGoal('Testing', ball_x=-1120, ball_y=500, ball_z=92, ball_vx=1000, ball_vy=-500, car_x=0, car_y=-1800),
    # StoneShotOnGoal('Testing', ball_x=-1120, ball_y=-500, ball_z=92, ball_vx=1000, ball_vy=400, car_x=0, car_y=-1800),

    # StoneShotOnGoal('Jump Shot', ball_x=-1120, ball_y=-500, ball_z=92, ball_vx=1000, ball_vy=400, car_x=0, car_y=-1800),

    # StoneShotOnGoal('Testing', ball_x=0, ball_y=1000, ball_z=92, car_x=0, car_y=-2200),
    # StoneShotOnGoal('Testing', ball_x=-10, ball_y=10, ball_z=92, car_x=0, car_y=-1800),
    # StoneShotOnGoal('Testing', ball_x=20, ball_y=10, ball_z=92, car_x=0, car_y=-1800),
    # StoneShotOnGoal('Testing', ball_x=50, ball_y=10, ball_z=92, car_x=0, car_y=-1800),
    # StoneShotOnGoal('Testing', ball_x=80, ball_y=10, ball_z=92, car_x=0, car_y=-1800),
    # StoneShotOnGoal('Testing', ball_x=110, ball_y=10, ball_z=92, car_x=0, car_y=-1800),

    # Pretend it's at 5500, 0
    # Car starts at 1500, -4000
    # StoneShotOnGoal('Wall shot', ball_x=4000, ball_y=0, ball_z=1500, car_x=1500, car_y=-4000, car_spin=pi/4),

    # StoneShotOnGoal('Hook Shot', ball_x=-1000, ball_y=3500, ball_z=100, car_x=1000, car_y=-0),
    # StoneShotOnGoal('Hook Shot', ball_x=-1000, ball_y=3500, ball_z=100, car_x=1000, car_y=-0, car_spin=pi),
    # StoneShotOnGoal('Hook Shot', ball_x=-2500, ball_y=2500, ball_z=100, car_x=-2000, car_y=-0, car_spin=pi),

    # StoneShotOnGoal('Testing', ball_x=-1000, ball_y=3500, ball_z=100, ball_vx=2000, car_x=1000, car_y=-0),
    # StoneShotOnGoal('Testing', ball_x=-2500, ball_y=2000, ball_z=100, ball_vx=200, ball_vy=1500, car_x=1000,
    #                 car_y=-0),

    # StoneShotOnGoal('Testing', ball_x=1000, ball_y=0, ball_z=2107, car_x=1000, car_y=-0),
    # StoneShotOnGoal('Testing', ball_x=1000, ball_y=0, ball_z=2108, car_x=1000, car_y=-0),

    # BallDrop('Rolling drop'),
    # BallDrop('Small drop', ball_y=10, ball_z=500),
    # BallDrop('Small drop', ball_y=20, ball_z=500),
    # BallDrop('Small drop', ball_y=30, ball_z=500),
    # BallDrop('Small drop', ball_y=40, ball_z=500),
    # BallDrop('Small drop', ball_y=50, ball_z=500),
    # BallDrop('Small drop', ball_y=60, ball_z=1000),
    # BallDrop('Small drop', ball_y=70, ball_z=500),

    # StoneShotOnGoal('Testing', ball_x=1000, ball_y=0, ball_z=1000, ball_vz=-1000, car_x=1000, car_y=-0),
    # StoneShotOnGoal('Testing', ball_x=1000, ball_y=0, ball_z=1000, ball_vz=-2000, car_x=1000, car_y=-0),
    # StoneShotOnGoal('Testing', ball_x=1000, ball_y=0, ball_z=1000, ball_vz=-3000, car_x=1000, car_y=-0),
    # StoneShotOnGoal('Testing', ball_x=1000, ball_y=0, ball_z=1000, ball_vz=-4000, car_x=1000, car_y=-0),
]
