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

TOP =	$(DYNCALL_HOME)
include $(DYNCALL_HOME)/buildsys/gmake/prolog.gmake

TARGET_DLL	 = test
INSTALL_LIB	 = 1

# uses some dyncall headers - no other dependencies.

UNITS += test

CPPFLAGS	+= -fvisibility=hidden -fvisibility-inlines-hidden 
LDFLAGS		+= -fvisibility=hidden -fvisibility-inlines-hidden 

ifdef BUILD_OS_darwin
CPPFLAGS	+=	-I/System/Library/Frameworks/JavaVM.framework/Versions/Current/Headers/
endif

LINK_LIBS	+= stdc++

include $(DYNCALL_HOME)/buildsys/gmake/epilog.gmake
