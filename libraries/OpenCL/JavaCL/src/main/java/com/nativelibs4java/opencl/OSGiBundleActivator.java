package com.nativelibs4java.opencl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class OSGiBundleActivator implements BundleActivator {
	
	public void start(BundleContext bundleContext) {
		System.out.println("Starting JavaCL");
	}
	
	public void stop(BundleContext bundleContext) {
		System.out.println("Stopping JavaCL");
	}
}
