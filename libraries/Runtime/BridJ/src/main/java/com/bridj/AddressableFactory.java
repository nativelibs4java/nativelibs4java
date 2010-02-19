package com.bridj;

interface AddressableFactory<T extends PointerRefreshable> {
    T newInstance(long address);
}