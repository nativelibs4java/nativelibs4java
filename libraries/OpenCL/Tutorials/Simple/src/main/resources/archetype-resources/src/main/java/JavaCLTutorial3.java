package ${package};

import com.nativelibs4java.opencl.*;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.util.*;
import com.nativelibs4java.util.*;
import org.bridj.Pointer;
import static org.bridj.Pointer.*;
import static java.lang.Math.*;
import java.io.IOException;

public class JavaCLTutorial3 {
    public static void main(String[] args) throws IOException {
        CLContext context = JavaCL.createBestContext();
        CLQueue queue = context.createDefaultQueue();

        int n = 1024;
        
        // Create OpenCL input and output buffers
        CLBuffer<Float> 
            a = context.createFloatBuffer(Usage.InputOutput, n), // a and b and read AND written to
            b = context.createFloatBuffer(Usage.InputOutput, n),
            out = context.createFloatBuffer(Usage.Output, n);

        TutorialKernels kernels = new TutorialKernels(context);
		int[] globalSizes = new int[] { n };
		CLEvent fillEvt = kernels.fill_in_values(queue, a, b, n, globalSizes, null);
		CLEvent addEvt = kernels.add_floats(queue, a, b, out, n, globalSizes, null, fillEvt);
		
        Pointer<Float> outPtr = out.read(queue, addEvt); // blocks until add_floats finished

        // Print the first 10 output values :
        for (int i = 0; i < 10 && i < n; i++)
            System.out.println("out[" + i + "] = " + outPtr.get(i));
        
    }
}
