from pathlib import Path

from rlbot.matchconfig.match_config import PlayerConfig, Team

agc_blue = PlayerConfig.bot_config(
    Path(__file__).absolute().parent.parent / 'src' / 'main' / 'python' / 'local_version.cfg',
    Team.BLUE)

agc_orange = PlayerConfig.bot_config(
    Path(__file__).absolute().parent.parent / 'src' / 'main' / 'python' / 'local_version.cfg',
    Team.ORANGE)
