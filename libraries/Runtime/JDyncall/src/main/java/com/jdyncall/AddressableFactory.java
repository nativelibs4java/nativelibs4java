package com.jdyncall;

public interface AddressableFactory<T extends PointerRefreshable> {
    T newInstance(long address);
}