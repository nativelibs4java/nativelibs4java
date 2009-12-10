package com.nativelibs4java.opencl.generator;

import com.ochafik.lang.jnaerator.JNAerator;
import com.ochafik.lang.jnaerator.JNAeratorConfig;

public class JavaCLGenerator extends JNAerator {

    public JavaCLGenerator(JNAeratorConfig config) {
		super(config);
	}

    void setupDefines() {
        /*
        __OPENCL_VERSION__
        __ENDIAN_LITTLE__

        __IMAGE_SUPPORT__
        __FAST_RELAXED_MATH__
        */
    }
	public static void main(String[] args) {
		
	}
}
