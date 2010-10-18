@echo off
setlocal

call mvn package -Dmaven.test.skip=true

set VERSION=1.0-20101006-4
set TEMP_OUT=tmpOut
mkdir %TEMP_OUT%
mkdir %TEMP_OUT%\misc
mkdir %TEMP_OUT%\misc\scala-devel
mkdir %TEMP_OUT%\misc\scala-devel\plugins
copy target\scalacl-compiler-plugin-%VERSION%.jar %TEMP_OUT%\misc\scala-devel\plugins 
call sbaz pack scalacl-compiler-plugin %TEMP_OUT% --linkbase http://nativelibs4java.sourceforge.net/sbaz/scalacl/ --version %VERSION% --pack200 --outdir src/main/sbaz --descfile ABOUT
explorer src\main\sbaz
