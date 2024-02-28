@echo off
setlocal enabledelayedexpansion
cd %~dp0

set Target=jp.knowlsat.lstm.LSTM_Ammonia_00
cd %PredictLSTM_DIR%
javac -d %PredictLSTM_DIR%\bin %PredictLSTM_DIR%\src/jp/knowlsat/lstm/predict/*.java %PredictLSTM_DIR%\src/jp/knowlsat/lstm/*.java
java -cp %PredictLSTM_DIR%\bin %Target%

timeout /t 1500 > nul
