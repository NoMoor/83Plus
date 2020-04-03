from pathlib import Path

from rlbot.matchconfig.match_config import PlayerConfig, Team

__cfg_path = Path(__file__).absolute().parent.parent / 'src' / 'main' / 'python' / 'local_version.cfg'
agc_blue = PlayerConfig.bot_config(__cfg_path, Team.BLUE)
agc_orange = PlayerConfig.bot_config(__cfg_path, Team.ORANGE)
