package com.nativelibs4java.opencl.blas;

import com.nativelibs4java.util.NIOUtils;
import java.nio.DoubleBuffer;

public class Vector {

    DoubleBuffer buffer;
    int size;

    public Vector(int size) {
        super();
        this.size = size;
        buffer = NIOUtils.directDoubles(size);
    }

    public double get(int i) {
        return buffer.get(i);
    }

    public void set(int i, double value) {
        buffer.put(i, value);
    }

    public int size() {
        return size;
    }
}
