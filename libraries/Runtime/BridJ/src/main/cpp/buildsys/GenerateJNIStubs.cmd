@echo off
setlocal enabledelayedexpansion

"%JAVA_HOME%\bin\javah.exe" -d ..\bridj -classpath ..\..\..\..\target\classes com.bridj.JNI com.bridj.BridJ com.bridj.Pointer

