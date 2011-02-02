sudo apt-get install maven2 subversion patch gcc make openjdk-6-jdk
cd
mkdir src
cd src

if [[ -d "nativelibs4java" ]] ; then
	svn update nativelibs4java ;
else
	svn checkout https://nativelibs4java.googlecode.com/svn/trunk/libraries nativelibs4java --username olivier.chafik ;
fi

if [[ -d "dyncall" ]] ; then
	svn update dyncall ;
else
	svn co https://dyncall.org/svn/dyncall/trunk dyncall ;
fi

cd dyncall/dyncall
export DYNCALL_HOME="`pwd`"
patch -p5 < ../../nativelibs4java/Runtime/BridJ/src/main/cpp/bridj/dyncall.diff 

./configure

cd ../../nativelibs4java
cd Runtime/BridJ

sh BuildNative
