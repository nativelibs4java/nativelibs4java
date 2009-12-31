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

TOP =	$(DYNCALL_HOME)
include $(DYNCALL_HOME)/buildsys/gmake/prolog.gmake

TARGET_DLL	 = jdyncall
INSTALL_LIB	 = 1

# uses some dyncall headers - no other dependencies.

CPPFLAGS	 += -I$(DYNCALL_HOME)
CPPFLAGS	 += -I$(DYNCALL_HOME)/dyncall
CPPFLAGS	 += -I$(DYNCALL_HOME)/dyncallback
CPPFLAGS	 += -I$(DYNCALL_HOME)/dynload

INSTALL_HEADERS	+= CommonClassesAndMethods.h
INSTALL_HEADERS	+= JNIUtils.hpp
INSTALL_HEADERS	+= Options.hpp
INSTALL_HEADERS	+= RawNativeForwardCallback.h
INSTALL_HEADERS	+= com_nativelibs4java_runtime_DynCall.h
INSTALL_HEADERS	+= com_nativelibs4java_runtime_JNI.h
INSTALL_HEADERS	+= com_nativelibs4java_runtime_JNI_CallingConvention.h
INSTALL_HEADERS	+= com_nativelibs4java_runtime_Pointer.h
INSTALL_HEADERS	+= jdyncall.hpp
INSTALL_HEADERS	+= jmethodcall.h

UNITS += BindCallback
UNITS += CommonClassesAndMethods
UNITS += JNI
UNITS += JNIUtils
UNITS += JavaToNativeCallHandler
UNITS += MethodCallInfo
UNITS += Options
UNITS += Pointer
UNITS += RawNativeForwardCallback
UNITS += Signatures
UNITS += ValueTypes

ifdef BUILD_OS_darwin
CPPFLAGS	+=	-I/System/Library/Frameworks/JavaVM.framework/Versions/Current/Headers/
endif

LINK_DIRS	+= $(DYNCALL_HOME)/dyncall/$(BUILD_DIR) 
LINK_DIRS	+= $(DYNCALL_HOME)/dynload/$(BUILD_DIR) 
LINK_DIRS	+= $(DYNCALL_HOME)/dyncallback/$(BUILD_DIR) 
LINK_LIBS   += dyncall_s dynload_s dyncallback_s
LINK_LIBS	+= stdc++

include $(DYNCALL_HOME)/buildsys/gmake/epilog.gmake
