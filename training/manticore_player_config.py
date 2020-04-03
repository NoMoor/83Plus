from pathlib import Path

from rlbot.matchconfig.match_config import PlayerConfig, Team

__cfg_path = Path(
    __file__).absolute().parent.parent.parent / 'SpaceInvaders' / 'manticore' / 'manticore' / 'manticore.cfg'
manticore_blue = PlayerConfig.bot_config(__cfg_path, Team.BLUE)
manticore_orange = PlayerConfig.bot_config(__cfg_path, Team.ORANGE)
