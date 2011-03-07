@echo off
setlocal enabledelayedexpansion

pushd .
cd src\main\cpp\buildsys\vs2008

call SetEnv.cmd
call CopyRulesIfNeeded.cmd

pushd ..
call GenerateJNIStubs.cmd
popd

for %%C in (Release Debug) do (
	REM if exists "c:\Program Files (x86)" 
	call "c:\Program Files (x86)\Microsoft Visual Studio 9.0\VC\vcvarsall.bat" x86
	REM if not exists "c:\Program Files (x86)" call "c:\Program Files\Microsoft Visual Studio 9.0\VC\vcvarsall.bat" x86
	devenv /nologo /useenv /Build "%%C|Win32" bridj.sln
	echo.
	
	REM if exists "c:\Program Files (x86)" 
	call "c:\Program Files (x86)\Microsoft Visual Studio 9.0\VC\vcvarsall.bat" x86_amd64
	REM if not exists "c:\Program Files (x86)" call "c:\Program Files (x86)\Microsoft Visual Studio 9.0\VC\vcvarsall.bat" x86_amd64
	devenv /nologo /useenv /Build "%%C|x64" bridj.sln
	echo.
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

rem if "%CONFIG%" == "Debug" (

	copy %CONFIG%\bridj.pdb ..\..\..\resources\win32
	copy %CONFIG%\test.pdb ..\..\..\..\test\resources\win32
	
	copy x64\%CONFIG%\bridj.pdb ..\..\..\resources\win64
	copy x64\%CONFIG%\test.pdb ..\..\..\..\test\resources\win64
rem )

popd

if not "%1" == "nopause" pause
