/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bridj;

import java.util.RandomAccess;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import static org.bridj.Pointer.*;

class DefaultNativeList<T> implements NativeList<T> {

    
    final ListType type;
    final PointerIO<T> io;
    volatile Pointer<T> pointer;
    volatile long size;

    public Pointer<?> getPointer() {
        return pointer;
    }
    
    /**
     * Create a native list that uses the provided storage and implementation strategy
     * @param pointer
     * @param type Implementation type
     */
    DefaultNativeList(Pointer<T> pointer, ListType type) {
        if (pointer == null || type == null)
            throw new IllegalArgumentException("Cannot build a " + getClass().getSimpleName() + " with " + pointer + " and " + type);
        
        this.io = pointer.getIO("Cannot create a list out of untyped pointer " + pointer);
        this.type = type;
        this.size = pointer.getValidElements();
        this.pointer = pointer;
    }
    
    protected int safelyCastLongToInt(long i, String content) {
        if (i > Integer.MAX_VALUE)
            throw new RuntimeException(content + " is bigger than Java int's maximum value : " + i);
        
        return (int)i;
    }
    public int size() {
        return safelyCastLongToInt(size, "Size of the native list");
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean contains(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Iterator<T> iterator() {
        return pointer.iterator();
    }

    public Object[] toArray() {
        return pointer.validElements(size).toArray();
    }

    public <T> T[] toArray(T[] ts) {
        return pointer.validElements(size).toArray(ts);
    }

    protected void requireSize(long newSize) {
        if (newSize > pointer.getValidElements()) {
            switch (type) {
                case Dynamic:
                    long nextSize = newSize < 5 ? newSize + 1 : (long)(newSize * 1.6);
                    Pointer<T> newPointer = allocateArray(io, nextSize);
                    pointer.copyTo(newPointer);
                    pointer = newPointer;
                    break;
                case FixedCapacity:
                    throw new UnsupportedOperationException("This list has a fixed capacity, cannot grow its storage");
                case Unmodifiable:
                    // should not happen !
                    checkModifiable();
            }
        }
    }
    protected void checkModifiable() {
        if (type == ListType.Unmodifiable)
            throw new UnsupportedOperationException("This list is unmodifiable");
    }
    public boolean add(T e) {
        add(size, e);
        return true;
    }

    public boolean remove(Object o) {
        checkModifiable();
        long i = indexOf(o, true, 0);
        if (i < 0)
            return false;
        
        remove(i);
        return true;
    }

    public boolean containsAll(Collection<?> clctn) {
        for (Object o : clctn)
            if (!contains(o))
                return false;
        return true;
    }

    public boolean addAll(Collection<? extends T> clctn) {
        return addAll(size(), clctn);
    }

    public boolean addAll(long i, Collection<? extends T> clctn) {
        checkModifiable();
        if (i > size || i < 0)
            throw new IndexOutOfBoundsException("Invalid index : " + i + " (list has size " + size +")");
        
        int n = clctn.size();
        requireSize(size + n);
        for (T o : clctn)
            add(i++, o);
        return n != 0;
    }
    public boolean addAll(int i, Collection<? extends T> clctn) {
        return addAll((long)i, clctn);
    }

    public boolean removeAll(Collection<?> clctn) {
        checkModifiable();
        
        List<Long> indicesToRemove = new ArrayList<Long>();
        for (Object o : clctn) {
            long i = indexOf(o, false, 0);
            if (i >= 0)
                indicesToRemove.add(i);
        }
        return removeAllIndices(indicesToRemove);
    }

    public boolean retainAll(Collection<?> clctn) {
        checkModifiable();
        
        List<Long> indicesToRemove = new ArrayList<Long>();
        for (long i = 0; i < size; i++) {
            T e = pointer.get(i);
            if (!clctn.contains(e))
                indicesToRemove.add(i);
        }
        return removeAllIndices(indicesToRemove);
    }

    public void clear() {
        checkModifiable();
        size = 0;
    }

    public T get(int i) {
        if (i >= size || i < 0)
            throw new IndexOutOfBoundsException("Invalid index : " + i + " (list has size " + size +")");
        
        return pointer.get(i);
    }

    public T set(int i, T e) {
        checkModifiable();
        if (i >= size || i < 0)
            throw new IndexOutOfBoundsException("Invalid index : " + i + " (list has size " + size +")");
        
        T old = pointer.get(i);
        pointer.set(i, e);
        return old;
    }

    void add(long i, T e) {
        checkModifiable();
        if (i > size || i < 0)
            throw new IndexOutOfBoundsException("Invalid index : " + i + " (list has size " + size +")");
        requireSize(size + 1);
        if (i < size) {
            pointer.moveBytesAtOffsetTo(i, pointer, i + 1, size - i);
        }
        pointer.set(i, e);
        size++;
    }
    public void add(int i, T e) {
        add((long)i, e);
    }

    private boolean removeAllIndices(List<Long> indicesToRemove) {
        int n = indicesToRemove.size();
        for (int i = n; i-- != 0;) {
            remove(indicesToRemove.get(i));
        }
        return n > 0;
    }

    public T remove(long i) {
        checkModifiable();
        if (i >= size || i < 0)
            throw new IndexOutOfBoundsException("Invalid index : " + i + " (list has size " + size +")");
        T old = pointer.get(i);
        long targetSize = io.getTargetSize();
        pointer.moveBytesAtOffsetTo((i + 1) * targetSize, pointer, i * targetSize, targetSize);
        size--;
        return old;
    }

    public T remove(int i) {
        return remove((long)i);
    }

    long indexOf(Object o, boolean last, int offset) {
        Pointer<T> pointer = this.pointer;
        assert offset >= 0 && (last || offset > 0);
        if (offset > 0)
            pointer = pointer.next(offset);
        
        Pointer<T> needle = allocate(io);
        needle.set((T)o);
        Pointer<T> occurrence = last ? pointer.findLast(needle) : pointer.find(needle);
        if (occurrence == null)
            return -1;
        
        return occurrence.getPeer() - pointer.getPeer();
    }
    public int indexOf(Object o) {
        return safelyCastLongToInt(indexOf(o, false, 0), "Index of the object");
    }

    public int lastIndexOf(Object o) {
        return safelyCastLongToInt(indexOf(o, true, 0), "Last index of the object");
    }

    public ListIterator<T> listIterator() {
        return listIterator(0);
    }

    public ListIterator<T> listIterator(int i) {
        if (size == 0)
            return Collections.EMPTY_LIST.listIterator(0); // TODO fix this...
        return pointer.next(i).validElements(size).iterator();
    }

    public List<T> subList(int fromIndex, int toIndex) {
        return new SubList(fromIndex, toIndex);
    }
    private class SubList implements List<T> {
        final int fromIndex;
        int toIndex;
        SubList(int fromIndex, int toIndex) {
            if (fromIndex < 0 || toIndex < 0 || fromIndex > size || toIndex > size || fromIndex > toIndex)
                throw new IndexOutOfBoundsException("Invalid sub list fromIndex and toIndex : " + fromIndex + " and " + toIndex + " (list size is " + size + ")");
            this.fromIndex = fromIndex;
            this.toIndex = toIndex;
        }

        public int size() {
            return toIndex - fromIndex;
        }

        public boolean isEmpty() {
            return size <= fromIndex;
        }

        public boolean contains(Object o) {
            return DefaultNativeList.this.indexOf(o, false, fromIndex) < size();
        }

        public Iterator<T> iterator() {
            return listIterator();
        }

        public Object[] toArray() {
            return pointer.next(fromIndex).validElements(size()).toArray();
        }

        public <T> T[] toArray(T[] ts) {
            return pointer.next(fromIndex).validElements(size()).toArray(ts);
        }

        public boolean add(T e) {
            add(size(), e);
            return true;
        }

        public boolean remove(Object o) {
            int i = indexOf(o);
            if (i < 0)
                return false;
            
            DefaultNativeList.this.remove(fromIndex + i);
            return true;
        }

        public boolean containsAll(Collection<?> clctn) {
            for (Object o : clctn)
                if (!contains(o))
                    return false;
            return true;
        }

        public boolean addAll(Collection<? extends T> clctn) {
            return addAll(0, clctn);
        }

        public boolean addAll(int i, Collection<? extends T> clctn) {
            for (T o : clctn)
                add(i++, o);
            return !clctn.isEmpty();
        }

        public boolean removeAll(Collection<?> clctn) {
            boolean changed = false;
            for (Object o : clctn)
                changed = remove(o) || changed;
            return changed;
        }

        public boolean retainAll(Collection<?> clctn) {
            
            boolean changed = false;
            for (int i = size(); i-- != 0;) {
                T e = get(i);
                if (!clctn.contains(e)) {
                    remove(i);
                    changed = true;
                }
            }
            return changed;
        }

        public void clear() {
            for (int i = size(); i-- > 0;)
                remove(i);
        }

        public T get(int i) {
            checkIndex(i);
            return DefaultNativeList.this.get(fromIndex + i);
        }

        public T set(int i, T e) {
            checkIndex(i);
            return DefaultNativeList.this.set(fromIndex + i, e);
        }

        public void add(int i, T e) {
            DefaultNativeList.this.add(i + toIndex++, e);
        }

        void checkIndex(int i) {
            if (i < 0 || i >= size())
                throw new IndexOutOfBoundsException("Invalid index : " + i + " (sub list is of size " + size() + ")");
            
        }
        public T remove(int i) {
            checkIndex(i);
            T old = get(i);
            DefaultNativeList.this.remove(i + fromIndex);
            toIndex--;
            return old;
        }

        public int indexOf(Object o) {
            long i = DefaultNativeList.this.indexOf(o, false, fromIndex);
            if (i < 0 || i >= toIndex)
                return -1;
            return (int)i - fromIndex;
        }

        public int lastIndexOf(Object o) {
            long i = DefaultNativeList.this.indexOf(o, true, 0); // TODO handle from index here
            if (i < 0 || i < fromIndex || i >= toIndex)
                return -1;
            return (int)i - fromIndex;
        }

        public ListIterator<T> listIterator() {
            return listIterator(0);
        }

        public ListIterator<T> listIterator(int i) {
            return pointer.next(fromIndex + i).validElements(size()).iterator();
        }

        public List<T> subList(int fromIndex2, int toIndex2) {
            int s = size();
            if (fromIndex2 < 0 || fromIndex2 > s || toIndex2 < 0 || toIndex2 >= s || fromIndex2 > toIndex2)
                throw new IndexOutOfBoundsException("Invalid sub list fromIndex and toIndex : " + fromIndex2 + " and " + toIndex2 + " (current sub list size is " + s + ")");
            
            return new SubList(fromIndex + fromIndex2, fromIndex + toIndex2);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof List))
            return false;
        
        int i = 0;
        List list = (List)o;
        
        // Fail fast for random access lists (fast size()) :
        if (list instanceof RandomAccess)
            if (size() != list.size())
                return false;
        
        for (Object other : list) {
            if (i >= size)
                return false;
            
            T e = get(i);
            if (e == null) {
                if (other != null)
                    return false;
            } else {
                if (!e.equals(o))
                    return false;
            }
            i++;
        }
        return true;
    }

    @Override
    public String toString() {
        return getClass().getName() + "{ " + Arrays.asList(toArray()) + "}";
    }
    
    
}
