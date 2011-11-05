#pragma once
#ifndef _OBJC_PROXY_H
#define _OBJC_PROXY_H

#include <jni.h>

#import <Cocoa/Cocoa.h>

@class DelgHolder;

@protocol Delg <NSObject>
	@required
	- (int)add:(int)a to:(int)b;
@end

@interface DelgHolder : NSObject 
	{
		id <Delg> delegate;
	}
	@property (nonatomic, assign) id <Delg> delegate;
	
	- (id) init;
	- (int)outerAdd:(int)a to:(int)b;
@end

@interface DelgImpl : NSObject <Delg>
	- (int)add:(int)a to:(int)b;
	- (id) init;
@end

#endif // _OBJC_PROXY_H
