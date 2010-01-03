#if ($useJNA.equals("true"))
#set ($package = "com.nativelibs4java.runtime.structs.jna")
#else
#set ($package = "com.nativelibs4java.runtime.structs")
#end

package $package;

import ${memoryClass};
import ${pointerClass};
import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Olivier Chafik
 */
public class Array<S extends Struct<S>> implements Iterable<S> {
    protected final Class<S> structClass;
    protected volatile Pointer pointer;
    protected final int size, structSize;

    protected volatile S[] cachedInstances;
    protected volatile S sharedInstance;
    protected final Constructor structConstructor;

    public Array(Class<S> structClass, int size, Pointer pointer) {
        this.structClass = structClass;
        this.size = size;
        StructIO<S> io = StructIO.getInstance(structClass);
        io.build();
        this.structSize = io.getStructSize();
        this.pointer = pointer == null ? new Memory(size * structSize) : pointer;
        try {
            this.structConstructor = structClass.getConstructor();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to find default constructor of struct " + structClass.getName(), ex);
        }
    }

	public synchronized void setPointer(Pointer pointer) {
		this.pointer = pointer;
	}
	public synchronized Pointer getPointer() {
		return pointer;
	}
    public synchronized S get(int index) {
        if (cachedInstances == null)
            cachedInstances = (S[])java.lang.reflect.Array.newInstance(structClass, size);
        S instance = cachedInstances[index];
        if (instance == null) {
            try {
                cachedInstances[index] = instance = ((S)structConstructor.newInstance()).setPointer(pointer.share(index * structSize));
            } catch (Exception ex) {
                throw new RuntimeException("Failed to create instance of struct " + structClass.getName(), ex);
            }
        }
        return instance;
    }

    public synchronized S getShared(int index) {
        if (sharedInstance == null) {
            try {
                sharedInstance = (S)structConstructor.newInstance();
            } catch (Exception ex) {
                throw new RuntimeException("Failed to create instance of struct " + structClass.getName(), ex);
            }
        }
        return sharedInstance.setPointer(pointer.share(index * structSize));
    }
    @Override
    public ListIterator<S> iterator() {
        try {
            final S instance = (S)structConstructor.newInstance();
            return new ListIterator<S>() {
                int index = -1;
                @Override
                public boolean hasNext() {
                    return index < size - 1;
                }

                @Override
                public S next() {
                    index = nextIndex();
                    return instance.setPointer(pointer.share(index * structSize));
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("Cannot remove structs from an array.");
                }

                @Override
                public boolean hasPrevious() {
                    return index > 0 || index == -1 && size > 0;
                }

                @Override
                public S previous() {
                    index = previousIndex();
                    return instance.setPointer(pointer.share(index * structSize));
                }

                @Override
                public int nextIndex() {
                    if (!hasNext())
                        throw new NoSuchElementException("Array has size " + size);
                    return index + 1;
                }

                @Override
                public int previousIndex() {
                    if (!hasPrevious())
                        throw new NoSuchElementException("Array has size " + size);
                    return (index == -1 ? size : index) - 1;
                }

                @Override
                public void set(S e) {
                    if (index < 0)
                        throw new UnsupportedOperationException("Iterator must be initialized with next() or previous() before calling set(...)");
                    instance.getPointer().getByteBuffer(0, structSize).put(e.getPointer().getByteBuffer(0, structSize));
                }

                @Override
                public void add(S e) {
                    throw new UnsupportedOperationException("Cannot add anything to a struct array !");
                }
            };
        } catch (Exception ex) {
            throw new RuntimeException("Failed to create instance of struct " + structClass.getName(), ex);
        }
    }


}
