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

DLL_PREFIX=lib
LD=gcc
CXX=gcc

COMPILE_PIC = 1

TOP =	$(DYNCALL_HOME)/dyncall
include $(DYNCALL_HOME)/dyncall/buildsys/gmake/prolog.gmake

TARGET_DLL	 = javacl
INSTALL_LIB	 = 1

# uses some dyncall headers - no other dependencies.

UNITS += API
UNITS += Proxy
UNITS += Library

CPPFLAGS += -Werror
CPPFLAGS += -std=c99
#CFLAGS += -g

# Lower requirement for GLIBC version number (2.2.5) :
# objdump -p src/main/resources/org/bridj/lib/linux_x64/bridj.so | fgrep 'Version References:' -A 100
CPPFLAGS += -U_FORTIFY_SOURCE

ifdef BUILD_OS_darwin
#CPPFLAGS  += -I/Library/Java/JavaVirtualMachines/1.7.0.jdk/Contents/Home/include 
#CPPFLAGS	+= -I/System/Library/Frameworks/JavaVM.framework/Versions/Current/Headers/
CPPFLAGS  += -I/Library/Java/JavaVirtualMachines/jdk1.7.0_13.jdk/Contents/Home/include
CPPFLAGS  += -I/Library/Java/JavaVirtualMachines/jdk1.7.0_13.jdk/Contents/Home/include/darwin
LDFLAGS  += -L/Library/Java/JavaVirtualMachines/jdk1.7.0_13.jdk/Contents/Home/jre/lib/jli -ljli
LDFLAGS		+= -ldl -framework Foundation -all_load 
endif

ifdef BUILD_OS_linux
CPPFLAGS	+=	-I/usr/lib/jvm/java-6-sun/include/
CPPFLAGS	+=	-I/usr/lib/jvm/java-6-sun/include/linux
CPPFLAGS	+=	-I/usr/lib/jvm/java-6-openjdk/include/
CPPFLAGS	+=	-I/usr/lib/jvm/java-6-openjdk/include/linux
CPPFLAGS  +=      -I/usr/lib/jvm/java-6-openjdk-armhf/include
CPPFLAGS  +=      -I/usr/lib/jvm/java-6-openjdk-armhf/include/linux
CPPFLAGS  +=      -I/usr/lib/jvm/java-6-openjdk-armel/include
CPPFLAGS  +=      -I/usr/lib/jvm/java-6-openjdk-armel/include/linux
endif

ifdef BUILD_OS_sunos
# http://gcc.gnu.org/onlinedocs/gcc/Solaris-2-Options.html
CPPFLAGS	+=	-I/usr/jdk/instances/jdk1.5.0/include
CPPFLAGS	+=	-I/usr/jdk/instances/jdk1.5.0/include/solaris
LDFLAGS		+= 	-mimpure-text
LDFLAGS		+=	-L/opt/csw/gcc4/lib
endif                 

ifdef BUILD_OS_darwin
CPPFLAGS += -mmacosx-version-min=10.4
LDFLAGS += -mmacosx-version-min=10.4
endif

ifdef BUILD_CONFIG_debug
CPPFLAGS +=	-O0 -g
else
CPPFLAGS += -O3
endif

include $(DYNCALL_HOME)/dyncall/buildsys/gmake/epilog.gmake
