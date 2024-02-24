@echo off
setlocal enabledelayedexpansion
cd %~dp0

set TestDir="C:\AllFiles\AI_Workspace\PredictLSTM"
set Target=jp.knowlsat.lstm.LSTM_Ammonia_30

cd %TestDir%

javac -d %TestDir%\bin %TestDir%\src/jp/knowlsat/lstm/predict/*.java %TestDir%\src/jp/knowlsat/lstm/*.java
java -cp %TestDir%\bin %Target%

timeout /t 1500 > nul
