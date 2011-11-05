#include "objctest.h"

@implementation DelgHolder

	@synthesize delegate;
	
	- (int)outerAdd:(int)a to:(int)b {
		return [[self delegate] add: a to: b];	
	}
	- (id) init
	{
		self = [super init];
		delegate = nil;
		return self;
	}
@end

@implementation DelgImpl
	- (int)add:(int)a to:(int)b {
		return a + b;
	}
	- (id) init
	{
		self = [super init]; 
		return self;
	}
@end

//Foo *obj = [[Foo alloc] init];
//[obj setDelegate:self];

