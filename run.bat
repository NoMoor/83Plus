@echo off

@rem Change the working directory to the location of this file so that relative paths will work
cd /C "%~dp0"

@rem Make sure the environment variables are up-to-date. This is useful if the user installed python a moment ago.
call ./RefreshEnv.cmd

python run.py

pause
