
mkdir src
mkdir bin

cd src

svn co https://dyncall.org/svn/dyncall/trunk dyncall
svn co https://nativelibs4java.googlecode.com/svn/trunk/libraries nativelibs4java

cd dyncall
echo "export DYNCALL_HOME=\"`pwd`\"" >> ~/.bashrc

cat ../nativelibs4java/Runtime/BridJ/src/main/cpp/bridj/dyncall.diff | sed 's/~\/src\/dyncall\///' | patch -p0

cd
cd bin

export MAVEN_VERSION=2.2.1
wget http://www.apache.org/dyn/closer.cgi/maven/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz
tar zxvf apache-maven-$MAVEN_VERSION-bin.tar.gz
echo "export PATH=`pwd`/apache-maven-$MAVEN_VERSION/bin:\$PATH" >> ~/.bashrc
chmod +x apache-maven-$MAVEN_VERSION/bin/*

cd
bash

cd src/nativelibs4java
mvn install -DskipTests
