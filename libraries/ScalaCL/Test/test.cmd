@echo off
setlocal enabledelayedexpansion

call mvn clean scala:compile || goto :eof

set NAME=%~1
set OUT_FILE=%~2
if "%OUT_FILE%" == "" set OUT_FILE=out.txt

echo Think of deleting "%OUT_FILE%" from time to time 
rem del "%OUT_FILE%"

set LOOPS=10
for /L %%L IN (1, 1, %LOOPS%) do (
	echo Run %%L / %LOOPS% 
	call :runTest
)

goto :eof

:runTest
call scala -cp target\classes Test %NAME% >> "%OUT_FILE%"
goto :eof
