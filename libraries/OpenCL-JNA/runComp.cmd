@echo off
setlocal enabledelayedexpansion

set M2REPO=\Users\Olivier\.m2\repository
pushd .
cd target\classes
scala -classpath %M2REPO%\com\jnaerator\jnaerator-runtime\0.9.3-SNAPSHOT\jnaerator-runtime-0.9.3-SNAPSHOT.jar;.  scalacl.ScalaCLTestRun

popd
