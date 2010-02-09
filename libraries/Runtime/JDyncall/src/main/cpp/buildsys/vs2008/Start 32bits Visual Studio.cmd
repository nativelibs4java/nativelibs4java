@echo off
call SetEnv.cmd
setlocal enabledelayedexpansion
call CopyRulesIfNeeded.cmd

start bridj.sln