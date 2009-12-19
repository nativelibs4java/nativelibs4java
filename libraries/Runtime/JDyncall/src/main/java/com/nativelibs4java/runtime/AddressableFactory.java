package com.nativelibs4java.runtime;

public interface AddressableFactory<T extends Addressable> {

    T newInstance(long address);
}
