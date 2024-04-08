@echo off
setlocal enabledelayedexpansion

set BatDIR=%cd%
cd %BatDIR%\..
set Target=jp.knowlsat.lstm.LSTM_Ammonia_30

javac -d bin src/jp/knowlsat/lstm/predict/*.java src/jp/knowlsat/lstm/*.java
java -cp bin %Target%

timeout /t 1500 > nul
