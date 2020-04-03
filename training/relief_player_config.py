from pathlib import Path

from rlbot.matchconfig.match_config import PlayerConfig, Team

__cfg_path = Path(__file__).absolute().parent.parent.parent / 'SpaceInvaders' / 'Relief' / 'README' / 'relief_bot.cfg'
relief_blue = PlayerConfig.bot_config(__cfg_path, Team.BLUE)
relief_orange = PlayerConfig.bot_config(__cfg_path, Team.ORANGE)
