@echo off
setlocal enabledelayedexpansion

set BatDIR=%cd%
cd %BatDIR%\..

set PER=1.0
echo %PER% > setting\setting_per_Ammonia.txt

@echo Ammonia:%PER%

timeout /t 5 > nul