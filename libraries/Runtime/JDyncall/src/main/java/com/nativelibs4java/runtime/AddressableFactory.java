package com.nativelibs4java.runtime;

public interface AddressableFactory<T extends PointerRefreshable> {
    T newInstance(long address);
}