@echo off
setlocal enabledelayedexpansion

set BatDIR=%cd%
cd %BatDIR%\..
set Target=jp.knowlsat.lstm.PredictReport

javac -d bin src/jp/knowlsat/lstm/predict/*.java src/jp/knowlsat/lstm/*.java
java -cp bin %Target%

@echo done.

timeout /t 3 > nul
