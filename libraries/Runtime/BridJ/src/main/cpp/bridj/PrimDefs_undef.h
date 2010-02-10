#ifdef primName  
	#undef primName 	
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

#ifndef REORDER_HALVES
#define REORDER_HALVES(ptr, retType, halfType, shift, upperIndex, lowerIndex) \
	((((retType)((halfType*)ptr)[upperIndex]) << shift) | ((halfType*)ptr)[lowerIndex])
#endif

