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
	(\
		(((jshort)((unsigned char*)peer)[upperIndex]) << 8) | \ 
				  ((unsigned char*)peer)[lowerIndex] \
	)
#endif

#ifndef REORDER_VALUE_BYTES_jint
#define REORDER_VALUE_BYTES_jint(peer, idx0, idx1, idx2, idx3) \
	(\
		(((jint)((unsigned char*)peer)[idx3]) << 24) | \
		(((jint)((unsigned char*)peer)[idx2]) << 16) | \
		(((jint)((unsigned char*)peer)[idx1]) << 8) | \
				((unsigned char*)peer)[idx0] \
	)
#endif

#ifndef REORDER_VALUE_BYTES_jlong
#define REORDER_VALUE_BYTES_jlong(peer, idx0, idx1, idx2, idx3, idx4, idx5, idx6, idx7) \
	(\
		(((jlong)((unsigned char*)peer)[idx7]) << 56) | \
		(((jlong)((unsigned char*)peer)[idx6]) << 48) | \
		(((jlong)((unsigned char*)peer)[idx5]) << 40) | \
		(((jlong)((unsigned char*)peer)[idx4]) << 32) | \
		(((jlong)((unsigned char*)peer)[idx3]) << 24) | \
		(((jlong)((unsigned char*)peer)[idx2]) << 16) | \
		(((jlong)((unsigned char*)peer)[idx1]) << 8) | \
				 ((unsigned char*)peer)[idx0] \
	)
#endif
