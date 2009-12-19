@echo off
call SetEnv.cmd
call CopyRulesIfNeeded.cmd
call "c:\Program Files (x86)\Microsoft Visual Studio 9.0\VC\vcvarsall.bat" x86_amd64

devenv /useenv jdyncall.sln
