@echo off
setlocal enabledelayedexpansion

for /D %%F in (. *) do (
	for %%S in (x64 Debug Release obj build) do (
		if exist "%%F\%%S" (
			echo Deleting "%%F\%%S"
			rmdir /S /Q "%%F\%%S"
		)
	)
	
)
del /Q *.ncb

if not "%1" == "nopause" pause
