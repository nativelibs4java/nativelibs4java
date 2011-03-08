DYNCALL_HOME=$1

if [[ -z "$DYNCALL_HOME" ]] ; then
	echo "Please provide a name for the dyncall checkout directory as first and unique argument"
	exit 1 ;
fi

if [[ -d "$$DYNCALL_HOME" ]] ; then
	echo "Directory $DYNCALL_HOME already exists."
	echo "Please backup or remove with 'rm -fR $DYNCALL_HOME' and retry (or use a different name)"
	exit 1 ;
fi

echo "Checking out dyncall to $DYNCALL_HOME..."
svn co https://dyncall.org/svn/dyncall/trunk $DYNCALL_HOME
cd $DYNCALL_HOME
echo "Retrieving BridJ's dyncall patches..."
svn export https://nativelibs4java.googlecode.com/svn/trunk/libraries/Runtime/BridJ/src/main/cpp/bridj/dyncall.diff
echo "Applying BridJ's dyncall patches..."
cat dyncall.diff | sed 's/~\/src\/dyncall\///' | patch -N -p0

cd dyncall
echo "Configuring..."
if [[ -d /System/Library/Frameworks/ ]] ; then sh ./configure --target-universal ; 
else sh ./configure ; fi

echo "Building..."
make
