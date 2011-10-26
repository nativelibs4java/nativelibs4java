#include "HandlersCommon.h"

#ifdef BRIDJ_OBJC_SUPPORT
#include <objc/objc.h>
#include <Block.h>
#include "bridj.hpp"

//http://cocoawithlove.com/2009/10/how-blocks-are-implemented-and.html
typedef struct Block_literal {
    void *isa;
 
    int flags;
    int reserved; // is actually the retain count of heap allocated blocks
 
    void (*invoke)(void *, ...); // a pointer to the block's compiled code
 
    struct Block_descriptor {
        unsigned long int reserved; // always nil
        unsigned long int size; // size of the entire Block_literal
         
        // functions used to copy and dispose of the block (if needed)
        void (*copy_helper)(void *dst, void *src);
        void (*dispose_helper)(void *src); 
    } *descriptor;
 
    // Here the struct contains one entry for every surrounding scope variable.
    // For non-pointers, these entries are the actual const values of the variables.
    // For pointers, there are a range of possibilities (__block pointer,
    // object pointer, weak pointer, ordinary pointer)
} Block_literal;	
	
const void* createObjCBlock() {
	void (^block)() = ^{
		// do nothing
	};
	return Block_copy(block);
}
jlong Java_org_bridj_objc_ObjCJNI_getObjCBlockFunctionPointer(JNIEnv* env, jclass cl, jlong jblock)
{
	Block_literal* block = (Block_literal*)JLONG_TO_PTR(jblock);
	return PTR_TO_JLONG(block->invoke);
}
jlong Java_org_bridj_objc_ObjCJNI_createObjCBlockWithFunctionPointer(JNIEnv* env, jclass cl, jlong fptr)
{
	Block_literal* block = (Block_literal*)createObjCBlock();
	block->invoke = JLONG_TO_PTR(fptr);
	return PTR_TO_JLONG(block);
}
void releaseObjCBlock(const void* block) {
	Block_release(block);	
}

#endif

