@echo off
setlocal enabledelayedexpansion

"%JAVA_HOME%\bin\javah.exe" -d ..\jdyncall -classpath ..\..\..\..\target\classes com.jdyncall.JNI com.jdyncall.DynCall com.jdyncall.Pointer

