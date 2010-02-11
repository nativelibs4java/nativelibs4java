#ifdef primName  
	#undef primName 	
#endif       

#ifdef halfJPrimName
	#undef halfJPrimName	
#endif       

#ifdef primCapName  
	#undef primCapName 
#endif              

#ifdef wrapperName  
	#undef wrapperName 
#endif              

#ifdef bufferName  
	#undef bufferName 	
#endif              

#ifdef primSize  
	#undef primSize	
#endif              

#ifdef jprimName  
	#undef jprimName	
#endif              

#ifdef jprimArray
	#undef jprimArray
#endif        

#ifdef primJNICapName
	#undef primJNICapName
#endif        

#ifdef alignmentMask
	#undef alignmentMask
#endif

#ifdef REORDER_VALUE_BYTES
	#undef REORDER_VALUE_BYTES
#endif

#ifdef REORDER_VALUE_BYTES_
	#undef REORDER_VALUE_BYTES_
#endif

#ifdef REORDER_VALUE_BYTES
	#undef REORDER_VALUE_BYTES
#endif

#ifndef REORDER_VALUE_BYTES_jshort
#define REORDER_VALUE_BYTES_jshort(peer, lowerIndex, upperIndex) \
	((((jshort)((jbyte*)peer)[upperIndex]) << 8) | ((jbyte*)peer)[lowerIndex])
#endif

#ifndef REORDER_VALUE_BYTES_jint
#define REORDER_VALUE_BYTES_jint(peer, idx0, idx1, idx2, idx3) \
	(\
		(((jint)((jbyte*)peer)[idx3]) << 24) | \
		(((jint)((jbyte*)peer)[idx2]) << 16) | \
		(((jint)((jbyte*)peer)[idx1]) << 8) | \
		((jbyte*)peer)[idx0] \
	)
#endif

#ifndef REORDER_VALUE_BYTES_jlong
#define REORDER_VALUE_BYTES_jlong(peer, idx0, idx1, idx2, idx3, idx4, idx5, idx6, idx7) \
	(\
		(((jlong)((jbyte*)peer)[idx7]) << 56) | \
		(((jlong)((jbyte*)peer)[idx6]) << 48) | \
		(((jlong)((jbyte*)peer)[idx5]) << 40) | \
		(((jlong)((jbyte*)peer)[idx4]) << 32) | \
		(((jlong)((jbyte*)peer)[idx3]) << 24) | \
		(((jlong)((jbyte*)peer)[idx2]) << 16) | \
		(((jlong)((jbyte*)peer)[idx1]) << 8) | \
		((jbyte*)peer)[idx0] \
	)
#endif
