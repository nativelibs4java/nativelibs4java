@echo off
setlocal enabledelayedexpansion

"%JAVA_HOME%\bin\javah.exe" -d ..\jdyncall\jni -classpath ..\..\..\..\target\classes com.nativelibs4java.runtime.JNI com.nativelibs4java.runtime.DynCall com.nativelibs4java.runtime.Pointer
