@echo off
setlocal
set INPUT=%~1
if "%INPUT%" == "" set INPUT=nl4j-runtime.zip
set OUTPUT=%INPUT%-shrunk.jar
set TEMP=shrinker-temp

mkdir %TEMP%
mkdir %TEMP%\out
mkdir %TEMP%\comp

javac -d %TEMP%\out -g:none JarShrinkerLoader.java
javac -d %TEMP%\comp SevenZip\Compress.java SevenZip\Decompress.java

java -Xmx1g -cp %TEMP%\comp SevenZip.Compress "%INPUT%" %TEMP%\out\classes.7z
echo Main-Class: JarShrinkerLoader> %TEMP%\Manifest.mf
jar cfm "%OUTPUT%" %TEMP%\Manifest.mf -C %TEMP%\out .

rmdir /S /Q %TEMP%
