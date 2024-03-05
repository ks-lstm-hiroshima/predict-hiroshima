@echo off
setlocal enabledelayedexpansion

set BatDIR=%cd%
cd %BatDIR%\..

set PER=0.5
echo %PER% > setting\setting_per_Ammonia.txt

@echo Ammonia:%PER%

timeout /t 5 > nul