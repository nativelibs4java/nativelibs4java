#Adapted from http://ildan.blogspot.com/2008/07/creating-universal-static-opencv.html

#export CONFFLAGS="--without-imageio --without-python --without-swig --disable-apps --disable-dependency-tracking --without-carbon --without-quicktime --enable-shared=yes --without-gtk"
export CONFFLAGS="--without-python --without-swig --disable-apps --disable-dependency-tracking --enable-shared=yes --without-gtk"

currDirName=`pwd | sed 's/^.*\///'` 
BUILDS_BASE_DIR=../$currDirName-build

FAT_ARCHS="
	x86_64
	i686
"
others="
	ppc
	x86-64
	i386
	armv6
	ppc64
"
FAT_LIBS="
	cv
	cxcore
	ml
	cvaux
	otherlibs/highgui
"

for arch in $FAT_ARCHS ; do
	rm -fR ../build/$arch 2> /dev/null
	mkdir $BUILDS_BASE_DIR 2> /dev/null
	mkdir $BUILDS_BASE_DIR/$arch 2> /dev/null
	cd $BUILDS_BASE_DIR/$arch
	
		CXX=
		CXXCPP=
		CXXFLAGS="-arch $arch"
		if [[ "$arch" == "x86_64" ]] ; then 
			CONFFLAGS="$CONFFLAGS --without-quicktime "
		fi
		host=$arch-apple-darwin9
		
		if [[ "$arch" == "armv6" ]] ; then 
			# Special handling of iPhone case (armv6) :
			CXX=/Developer/Platforms/iPhoneOS.platform/Developer/usr/bin/arm-apple-darwin9-g++-4.0.1 
			CXXFLAGS="-arch armv6 -isysroot /Developer/Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS2.0.sdk" 
			CXXCPP=/Developer/Platforms/iPhoneOS.platform/Developer/usr/bin/cpp
			host=arm-apple-darwin9 
			MAKE_ARGS= 
			CONFIG_ARGS=`
				if [[ -n "$CXX" ]] ; then echo "CXX=\"$CXX\"" ; fi
				if [[ -n "$CXXFLAGS" ]] ; then echo "CXXFLAGS=\"$CXXFLAGS\"" ; fi
				if [[ -n "$CXXCPP" ]] ; then echo "CXXCPP=\"$CXXCPP\"" ; fi`
			
			
		fi
		#make distclean
		../../$currDirName/configure ${CONFFLAGS} --host=$host CXX="$CXX" CXXFLAGS="$CXXFLAGS" CXXCPP="$CXXCPP" && make CXXFLAGS="$CXXFLAGS" | tee make.out

	cd ../../$currDirName ;
done

for lib in $FAT_LIBS ; do 
	libName=`echo $lib|sed 's/^.*\///'` 
	#lipo -create `for arch in $FAT_ARCHS ; do echo "$BUILDS_BASE_DIR/$arch/$lib/src/.libs/lib$libName.a " ; done` -output $BUILDS_BASE_DIR/lib$libName.a | tee $BUILDS_BASE_DIR/lipo.$libName.a.log
	for ext in a dylib ; do 
		#echo "Creating a fat lib$libName.$ext"
		INPUT_SLIM_FILES=`for arch in $FAT_ARCHS ; do 
			A_FILE_IN="$BUILDS_BASE_DIR/$arch/$lib/src/.libs/lib$libName.$ext" 
			if [[ -f "$A_FILE_IN" ]] ; then 
				echo $A_FILE_IN ; 
			fi ; 
		done | tr '\n' ' '`
		if [[ -z "$INPUT_SLIM_FILES" ]] ; then
			echo "ERROR: No input files for lib$libName.$ext !!!" ;
		else
			lipo -create $INPUT_SLIM_FILES -output $BUILDS_BASE_DIR/lib$libName.$ext | tee $BUILDS_BASE_DIR/lipo.lib$libName.$ext.log
			
			cp $BUILDS_BASE_DIR/lib$libName.$ext . ;
		fi
	done ;
done

#open $BUILDS_BASE_DIR

#make -j 2

echo "Done !"
