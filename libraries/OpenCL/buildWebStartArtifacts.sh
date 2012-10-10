#!/bin/bash

mvn -Pwebstart -Dstorepass=$KEYSTORE_PASS -DskipTests clean install || exit 1

rm -fR target-webstart
mkdir target-webstart
cd target-webstart

for SUB in InteractiveImageDemo Demos OpenGLDemos ; do
	cp ../$SUB/target/*-shaded.jar . || exit 1
done

open .
open /Applications/Cyberduck.app/ || exit 1

