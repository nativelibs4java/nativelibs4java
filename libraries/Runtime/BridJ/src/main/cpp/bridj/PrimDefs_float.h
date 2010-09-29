#include "PrimDefs_undef.h"
#define primName 		float
#define jprimName 		jfloat
#define jprimArray	jfloatArray
#define primJNICapName 	Float
#define primCapName 	Float
#define wrapperName 	Float
#define bufferName 		FloatBuffer
#define primSize		4
#define alignmentMask	3
#define TEMP_REORDER_VAR_TYPE jint

#ifndef BIG_ENDIAN
#define REORDER_VALUE_BYTES(peer) REORDER_VALUE_BYTES_jint(peer, 0, 1, 2, 3)
#else
#define REORDER_VALUE_BYTES(peer) REORDER_VALUE_BYTES_jint(peer, 3, 2, 1, 0)
#endif
