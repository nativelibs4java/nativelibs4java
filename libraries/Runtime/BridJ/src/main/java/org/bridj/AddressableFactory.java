package org.bridj;

interface AddressableFactory<T extends PointerRefreshable> {
    T newInstance(long address);
}