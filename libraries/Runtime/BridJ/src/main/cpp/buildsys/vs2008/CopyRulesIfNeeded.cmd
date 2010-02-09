@echo off
setlocal

set FILE=masm64.rules
set TARGET_FILE=C:\Program Files\Microsoft Visual Studio 9.0\VC\VCProjectDefaults\%FILE%

if not exist "%TARGET_FILE%" copy "%FILE%" "%TARGET_FILE%"
