
@echo off
setlocal enabledelayedexpansion

pushd ..\buildsys\vs2008\Debug

"%JAVA_HOME%\bin\java.exe" -classpath ..\..\..\jdyncall\classes jdyncall.Main

popd

pause
