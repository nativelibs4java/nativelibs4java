#parse("main/Header.vm")
package com.nativelibs4java.opencl;
import com.nativelibs4java.opencl.library.IOpenCLLibrary.cl_event;
import org.bridj.*;
import static org.bridj.Pointer.*;
/**
 *
 * @author ochafik
 */
final class ReusablePointers {
    public final ReusablePointer
            sizeT3_1 = new ReusablePointer(3 * SizeT.SIZE),
            sizeT3_2 = new ReusablePointer(3 * SizeT.SIZE),
            sizeT3_3 = new ReusablePointer(3 * SizeT.SIZE);
    
    public final Pointer<Integer>
            int1 = allocateInt().withoutValidityInformation(),
            int2 = allocateInt().withoutValidityInformation();
    
    public final Pointer<SizeT>
            sizeT1 = allocateSizeT().withoutValidityInformation();
    
    public final Pointer<Long>
            long1 = allocateLong().withoutValidityInformation();
    
    public final Pointer<Pointer<?>>
            ptr1 = allocatePointer().withoutValidityInformation();
    
    public final ReusablePointer
            int3_1 = new ReusablePointer(4 * 3);
    
    public final ReusablePointer
            kernelArg = new ReusablePointer(8 * 16); // double16 arguments !
    
    public final Pointer<cl_event> event_out = allocateTypedPointer(cl_event.class).withoutValidityInformation();
    
    public final Pointer<Integer> pErr = allocateInt().withoutValidityInformation();
    
    public final int[] event_count = new int[1];
    public final ReusablePointer events_in = new ReusablePointer(Pointer.SIZE * 10);
    
    private ReusablePointers() {}
    
    public static ReusablePointers get() {
        return local.get();
    }
    private static final ThreadLocal<ReusablePointers> local = new ThreadLocal<ReusablePointers>() {

        @Override
        protected ReusablePointers initialValue() {
            return new ReusablePointers();
        }
        
    };
    
}
