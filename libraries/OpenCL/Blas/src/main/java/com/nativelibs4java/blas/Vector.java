package com.nativelibs4java.blas;

import com.nativelibs4java.util.NIOUtils;
import java.nio.DoubleBuffer;

public abstract class Vector implements Data {

    protected final int size;

    public Vector(int size) {
        super();
        this.size = size;
    }

    public abstract double get(int i);

    public abstract void set(int i, double value);

    public int size() {
        return size;
    }
}
