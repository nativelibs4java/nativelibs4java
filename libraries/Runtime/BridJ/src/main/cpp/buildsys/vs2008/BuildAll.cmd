@echo off
setlocal enabledelayedexpansion

call SetEnv.cmd
call CopyRulesIfNeeded.cmd

pushd ..
call GenerateJNIStubs.cmd
popd

for %%C in (Debug Release) do (
	if exists "c:\Program Files (x86)" call "c:\Program Files (x86)\Microsoft Visual Studio 9.0\VC\vcvarsall.bat" x86
	if not exists "c:\Program Files (x86)" call "c:\Program Files\Microsoft Visual Studio 9.0\VC\vcvarsall.bat" x86
	devenv /useenv /Build "%%C|Win32" bridj.sln
	
	if exists "c:\Program Files (x86)" call "c:\Program Files (x86)\Microsoft Visual Studio 9.0\VC\vcvarsall.bat" x86_amd64
	if not exists "c:\Program Files (x86)" call "c:\Program Files (x86)\Microsoft Visual Studio 9.0\VC\vcvarsall.bat" x86_amd64
	devenv /useenv /Build "%%C|x64" bridj.sln
)

for %%T in (win32 win64) do (
	del ..\..\..\resources\%%T\*.dll
	del ..\..\..\..\test\resources\%%T\*.dll
)

rem set CONFIG=Debug
set CONFIG=Release

copy %CONFIG%\bridj.dll ..\..\..\resources\win32
copy %CONFIG%\test.dll ..\..\..\..\test\resources\win32

copy x64\%CONFIG%\bridj.dll ..\..\..\resources\win64
copy x64\%CONFIG%\test.dll ..\..\..\..\test\resources\win64

if not "%1" == "nopause" pause
