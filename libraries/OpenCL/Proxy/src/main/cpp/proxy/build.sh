export DYNCALL_HOME=../../../../../../BridJ/dyncall

set -e

cd $(dirname $0)
CURR="`pwd`"

export MAKE_CMD=make
if [[ "`which gmake`" != "" ]] ; then
	export MAKE_CMD=gmake ;
fi

cd "$DYNCALL_HOME/dyncall"

sh ./configure
#if [[ -d /System/Library/Frameworks/ && ! -d /Applications/MobilePhone.app ]] ; then
#  # Avoid LC_DYLD_INFO (https://discussions.apple.com/thread/3197542?start=0&tstart=0)
#  export MACOSX_DEPLOYMENT_TARGET=10.4
#  sh ./configure --target-universal
#else 
#  sh ./configure
#fi

cd "$CURR"
$MAKE_CMD $@
