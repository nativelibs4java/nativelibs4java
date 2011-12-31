#!/usr/bin

rm -fR target/tempCompile

mkdir -p target/tempCompile
javac -d target/tempCompile/ -cp src/main/java/ src/main/java/JarShrinkerLoader.java
cd target/tempCompile/ ; find * -type f > ../../src/main/resources/runtime.list ; cd ../..
