#include "PrimDefs_undef.h"
#define primName 		int
#define jprimName 		jint
#define jprimArray 		jintArray
#define primJNICapName 	Int
#define primCapName 	Int
#define wrapperName 	Integer
#define bufferName 		IntBuffer
#define primSize		4
#define alignmentMask	3
#ifdef LITTLE_ENDIAN
#define REORDER_VALUE_BYTES(peer) REORDER_HALVES(peer, jint, jshort, 16, 0, 1)
#else
#define REORDER_VALUE_BYTES(peer) REORDER_HALVES(peer, jint, jshort, 16, 1, 0)
#endif
