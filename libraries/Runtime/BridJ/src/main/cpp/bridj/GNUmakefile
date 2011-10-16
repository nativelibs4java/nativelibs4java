#//////////////////////////////////////////////////////////////////////////////
#
# Copyright (c) 2007,2009 Daniel Adler <dadler@uni-goettingen.de>, 
#                         Tassilo Philipp <tphilipp@potion-studios.com>
#
# Permission to use, copy, modify, and distribute this software for any
# purpose with or without fee is hereby granted, provided that the above
# copyright notice and this permission notice appear in all copies.
#
# THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
# WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
# MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
# ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
# WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
# ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
# OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
#
#//////////////////////////////////////////////////////////////////////////////

#BUILD_CONFIG=debug

LD=gcc
CXX=gcc

COMPILE_PIC = 1

TOP =	$(DYNCALL_HOME)
include $(DYNCALL_HOME)/buildsys/gmake/prolog.gmake

TARGET_DLL	 = bridj
INSTALL_LIB	 = 1

# uses some dyncall headers - no other dependencies.

CPPFLAGS	 += -I$(DYNCALL_HOME)
CPPFLAGS	 += -I$(DYNCALL_HOME)/dyncall
CPPFLAGS	 += -I$(DYNCALL_HOME)/dyncallback
CPPFLAGS	 += -I$(DYNCALL_HOME)/dynload

UNITS += Exceptions
UNITS += JNI
UNITS += Platform
UNITS += Pointer

UNITS += HandlersCommon 
UNITS += GenericJavaCallbackHandlers
UNITS += CallbackHandler
UNITS += CppHandler
UNITS += FunctionHandler
#UNITS += StructHandler
UNITS += CallbackHandler
UNITS += ThreadLocal
ifdef BUILD_OS_darwin
UNITS += ObjCHandler
UNITS += ObjCProxy
endif
ifdef BUILD_OS_iphoneos
UNITS += ObjCHandler
endif
UNITS += RawNativeForwardCallback
#ifdef BUILD_TOOL_gcc
UNITS += RawNativeForwardCallback_arch
#endif

CPPFLAGS += -std=c99
#CFLAGS += -msse2 -msseregparm
#CFLAGS += -g

ifdef BUILD_OS_darwin
CPPFLAGS		+= -I/System/Library/Frameworks/JavaVM.framework/Versions/Current/Headers/
CPPFLAGS 	+= -DBRIDJ_OBJC_SUPPORT
LDFLAGS		+= -ldl -all_load
LDFLAGS		+= -framework Foundation
#LDFLAGS		+= -lobjc 
endif

ifdef BUILD_OS_iphoneos
CPPFLAGS		+= -I/Users/ochafik/src/jamvm/src
CPPFLAGS 	+= -DBRIDJ_OBJC_SUPPORT
LDFLAGS		+= -ldl -lobjc -all_load
endif

ifndef BUILD_OS_android
ifndef BUILD_ARCH_arm
LDFLAGS		+= -lpthread
endif
endif

ifdef BUILD_OS_linux
CPPFLAGS	+=	-I/usr/lib/jvm/java-6-sun/include/
CPPFLAGS	+=	-I/usr/lib/jvm/java-6-sun/include/linux
CPPFLAGS	+=	-I/usr/lib/jvm/java-6-openjdk/include/
CPPFLAGS	+=	-I/usr/lib/jvm/java-6-openjdk/include/linux
endif

ifdef BUILD_OS_sunos
# http://gcc.gnu.org/onlinedocs/gcc/Solaris-2-Options.html
CPPFLAGS	+=	-I/usr/jdk/instances/jdk1.5.0/include
CPPFLAGS	+=	-I/usr/jdk/instances/jdk1.5.0/include/solaris
LDFLAGS		+= 	-mimpure-text
LDFLAGS		+=	-L/opt/csw/gcc4/lib
endif                 

#CPPFLAGS += -O0

ifdef BUILD_OS_darwin
CPPFLAGS += -O3
CPPFLAGS += -mmacosx-version-min=10.4
LDFLAGS += -mmacosx-version-min=10.4
else
CPPFLAGS += -O2
endif

#LINK_LIBS	+= stdc++

#CPPFLAGS += -DJNICALL=__cdecl -DJNIEXPORT=__cdecl

#CPPFLAGS	+= -fvisibility=hidden -fvisibility-inlines-hidden 
#LDFLAGS		+= -fvisibility=hidden -fvisibility-inlines-hidden 

LINK_DIRS	+= $(DYNCALL_HOME)/dyncall/$(BUILD_DIR) 
LINK_DIRS	+= $(DYNCALL_HOME)/dynload/$(BUILD_DIR) 
LINK_DIRS	+= $(DYNCALL_HOME)/dyncallback/$(BUILD_DIR) 
LINK_LIBS   += dyncall_s dynload_s dyncallback_s

include $(DYNCALL_HOME)/buildsys/gmake/epilog.gmake
