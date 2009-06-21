export CONFFLAGS="--without-imageio --without-python --without-swig --disable-apps --disable-dependency-tracking --without-carbon --without-quicktime --enable-shared=no --without-gtk"

mkdir ppc
cd ppc
../../opencv/configure ${CONFFLAGS} --host=ppc-apple-darwin9
make clean
make CXXFLAGS="-arch ppc"
cd ..

mkdir i686
cd i686
../../opencv/configure ${CONFFLAGS} --host=i686-apple-darwin9
make clean
make CXXFLAGS="-arch i686"
cd ..

mkdir i386
cd i386
../../opencv/configure ${CONFFLAGS} --host=i386-apple-darwin9
make clean
make CXXFLAGS="-arch i386"
cd ..

# Taken from [http://ildan.blogspot.com/2008/07/creating-universal-static-opencv.html] :
mkdir armv6
cd armv6
../../opencv/configure ${CONFFLAGS} --host=arm-apple-darwin9 CXX=/Developer/Platforms/iPhoneOS.platform/Developer/usr/bin/arm-apple-darwin9-g++-4.0.1 CXXFLAGS="-arch armv6 -isysroot /Developer/Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS2.0.sdk" CXXCPP=/Developer/Platforms/iPhoneOS.platform/Developer/usr/bin/cpp
make clean
make
cd ..

ARCHS="
        ppc
        i686
        i386
        armv6
"
LIBS="
        cv
        cxcore
        ml
        otherlibs/cvaux
        otherlibs/highgui
"
for lib in $LIBS ; do 
        libName=`echo $lib|sed 's/^.*\///'` 
        echo lipo -create `for arch in $ARCHS ; do echo "$arch/$lib/src/.libs/lib$libName.a " ; done` -output lib$libName.a ;
done

make -j 2
