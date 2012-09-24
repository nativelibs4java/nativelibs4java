#parse("main/Header.vm")
package com.nativelibs4java.opencl;

class PlatformUtils {
	public enum PlatformKind {
		AMDApp,
		NVIDIA,
		Apple,
		Intel
	}
	public static PlatformKind guessPlatformKind(CLPlatform p) {
		String name = p.getName();
		if (name != null) {
			if (name.equals("Apple"))
				return PlatformKind.Apple;
			else if (name.equals("ATI Stream") || name.equals("AMD Accelerated Parallel Processing"))
				return PlatformKind.AMDApp;
			else {
				String vendor = p.getVendor().toLowerCase(); 
				if (vendor.contains("nvidia"))
					return PlatformKind.NVIDIA;
			}
		}
		return null;
	}
}
