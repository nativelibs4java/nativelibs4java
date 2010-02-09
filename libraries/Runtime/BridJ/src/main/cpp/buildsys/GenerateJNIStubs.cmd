@echo off
setlocal enabledelayedexpansion

"%JAVA_HOME%\bin\javah.exe" -d ..\bridj -classpath ..\..\..\..\target\classes com.bridj.JNI com.bridj.DynCall com.bridj.Pointer

