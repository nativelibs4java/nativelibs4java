package com.bridj;

public interface AddressableFactory<T extends PointerRefreshable> {
    T newInstance(long address);
}