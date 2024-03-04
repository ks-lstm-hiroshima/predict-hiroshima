@echo off
setlocal enabledelayedexpansion
cd %~dp0

cd %PredictLSTM_DIR%
set PER=0.7
echo %PER% > %PredictLSTM_DIR%\setting\setting_per_Ammonia.txt

@echo Ammonia:%PER%

timeout /t 5 > nul