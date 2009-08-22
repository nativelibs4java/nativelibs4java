if not exist classes mkdir classes
javac -d classes -cp ../Mono.jar *.java
java -cp ../Mono.jar;classes HelloDotNetWorld
pause
