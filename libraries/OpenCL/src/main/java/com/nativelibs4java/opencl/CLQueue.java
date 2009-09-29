/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
*/

package com.nativelibs4java.opencl;
import com.nativelibs4java.opencl.library.OpenCLLibrary;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.sun.jna.*;
import java.nio.*;
import static com.nativelibs4java.opencl.OpenCL4Java.*;

/**
 * OpenCL command queue.
 * A queue is bound to a single device.
 * @author ochafik
 *
 */
public class CLQueue extends CLEntity<cl_command_queue> {

    final CLContext context;

    CLQueue(CLContext context, cl_command_queue entity) {
        super(entity);
        this.context = context;
    }

    @Override
    protected void clear() {
        error(CL.clReleaseCommandQueue(get()));
    }
    /// Wait for the queue to be fully executed. Costly.

    public void finish() {
        error(CL.clFinish(get()));
    }
}