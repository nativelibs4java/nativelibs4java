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

svn revert --depth infinity .
for newFile in dyncall/dyncall_struct.c dyncall/dyncall_struct.h ; do
	rm $DYNCALL_HOME/$newFile
	touch $DYNCALL_HOME/$newFile
	svn add $DYNCALL_HOME/$newFile ;
done

cat ../../nativelibs4java/Runtime/BridJ/src/main/cpp/bridj/dyncall.diff | sed "s/~/${HOME//\//\\/}/" | patch -p6  

./configure

cd ../../nativelibs4java
cd Runtime/BridJ

sh BuildNative
