#include "PrimDefs_undef.h"
#define primName 		long
#define jprimName 		jlong
#define jprimArray 		jlongArray
#define primJNICapName 	Long
#define primCapName 	Long
#define wrapperName 	Long
#define bufferName 		LongBuffer
#define primSize		8
#define alignmentMask	7
#ifdef LITTLE_ENDIAN
#define REORDER_VALUE_BYTES(peer) REORDER_HALVES(peer, jlong, jint, 32, 0, 1)
#else
#define REORDER_VALUE_BYTES(peer) REORDER_HALVES(peer, jlong, jint, 32, 1, 0)
#endif
#ifdef LITTLE_ENDIAN
#define REORDER_VALUE_BYTES(peer) REORDER_HALVES(peer, jlong, jint, 32, 0, 1)
#else
#define REORDER_VALUE_BYTES(peer) REORDER_HALVES(peer, jlong, jint, 32, 1, 0)
#endif
