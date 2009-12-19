@echo off
setlocal enabledelayedexpansion

call SetEnv.cmd
call CopyRulesIfNeeded.cmd

pushd ..
call GenerateJNIStubs.cmd
popd

for %%C in (Debug Release) do (
	call "c:\Program Files (x86)\Microsoft Visual Studio 9.0\VC\vcvarsall.bat" x86
	devenv /useenv /Build "%%C|Win32" jdyncall.sln
	
	call "c:\Program Files (x86)\Microsoft Visual Studio 9.0\VC\vcvarsall.bat" x86_amd64
	devenv /useenv /Build "%%C|x64" jdyncall.sln
)

if not "%1" == "nopause" pause
