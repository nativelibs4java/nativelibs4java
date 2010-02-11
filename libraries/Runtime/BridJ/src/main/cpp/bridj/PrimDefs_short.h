#include "PrimDefs_undef.h"
#define primName 		short
#define jprimName 		jshort
#define jprimArray 		jshortArray
#define primJNICapName 	Short
#define primCapName 	Short
#define wrapperName 	Short
#define bufferName 		ShortBuffer
#define primSize		2
#define alignmentMask	1
#ifdef LITTLE_ENDIAN
#define REORDER_VALUE_BYTES(peer) REORDER_HALVES(peer, jshort, jbyte, 8, 0, 1)
#else
#define REORDER_VALUE_BYTES(peer) REORDER_HALVES(peer, jshort, jbyte, 8, 1, 0)
#endif
#ifdef LITTLE_ENDIAN
#define REORDER_VALUE_BYTES(peer) REORDER_HALVES(peer, jshort, jbyte, 8, 0, 1)
#else
#define REORDER_VALUE_BYTES(peer) REORDER_HALVES(peer, jshort, jbyte, 8, 1, 0)
#endif
