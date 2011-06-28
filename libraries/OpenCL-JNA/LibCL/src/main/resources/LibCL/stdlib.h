
//#include "tcl.h"
//#ifdef NO_STDLIB_H
//#   include "../compat/stdlib.h"
//#else
//#   include <stdlib.h>
//#endif
//#include <ctype.h>

#ifndef CONST
#define CONST const
#endif

#ifndef isspace
#define isspace(c) (c == ' ' || c == '\t' || c == '\r' || c == '\n')
#endif

#ifndef isdigit
#define isdigit(c) (c >= '0' && c <= '9')
#endif

