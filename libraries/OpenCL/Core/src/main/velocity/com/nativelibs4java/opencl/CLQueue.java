#parse("main/Header.vm")
package com.nativelibs4java.opencl;
import static com.nativelibs4java.opencl.CLException.error;
import static com.nativelibs4java.opencl.JavaCL.CL;
import static com.nativelibs4java.opencl.library.IOpenCLLibrary.CL_FALSE;
import static com.nativelibs4java.opencl.library.IOpenCLLibrary.CL_QUEUE_PROPERTIES;
import static com.nativelibs4java.opencl.library.IOpenCLLibrary.CL_TRUE;

import java.util.EnumSet;

import com.nativelibs4java.opencl.library.IOpenCLLibrary.cl_command_queue;
import com.nativelibs4java.opencl.library.IOpenCLLibrary.cl_event;
import com.nativelibs4java.opencl.library.IOpenCLLibrary.cl_mem;
import org.bridj.*;
import static org.bridj.Pointer.*;

/**
 * OpenCL command queue.<br/>
 * OpenCL objects such as memory, program and kernel objects are created using a context. <br/>
 * Operations on these objects are performed using a command-queue. <br/>
 * The command-queue can be used to queue a set of operations (referred to as commands) in order. <br/>
 * Having multiple command-queues allows applications to queue multiple independent commands without requiring synchronization. <br/>
 * Note that this should work as long as these objects are not being shared. <br/>
 * Sharing of objects across multiple command-queues will require the application to perform appropriate synchronization.<br/>
 * <br/>
 * A queue is bound to a single device.
 * see {@link CLDevice#createQueue(com.nativelibs4java.opencl.CLContext, com.nativelibs4java.opencl.CLDevice.QueueProperties[]) } 
 * see {@link CLDevice#createOutOfOrderQueue(com.nativelibs4java.opencl.CLContext) }
 * see {@link CLDevice#createProfilingQueue(com.nativelibs4java.opencl.CLContext) }
 * see {@link CLContext#createDefaultQueue(com.nativelibs4java.opencl.CLDevice.QueueProperties[]) }
 * see {@link CLContext#createDefaultOutOfOrderQueue() }
 * see {@link CLContext#createDefaultProfilingQueue() }
 * @author Olivier Chafik
 *
 */
public class CLQueue extends CLAbstractEntity {

    #declareInfosGetter("infos", "CL.clGetCommandQueueInfo")

	final CLContext context;
	final CLDevice device;

    CLQueue(CLContext context, long entity, CLDevice device) {
        super(entity);
        this.context = context;
		this.device = device;
    }
    
    public CLContext getContext() {
        return context;
    }
	public CLDevice getDevice() {
		return device;
	}
	
	volatile Boolean outOfOrder;
	public synchronized boolean isOutOfOrder() {
		if (outOfOrder == null)
			outOfOrder = getProperties().contains(CLDevice.QueueProperties.OutOfOrderExecModeEnable);
		return outOfOrder;
	}

	@InfoName("CL_QUEUE_PROPERTIES")
	public EnumSet<CLDevice.QueueProperties> getProperties() {
		return CLDevice.QueueProperties.getEnumSet(infos.getIntOrLong(getEntity(), CL_QUEUE_PROPERTIES));
	}

	@SuppressWarnings("deprecation")
	public void setProperty(CLDevice.QueueProperties property, boolean enabled) {
		context.getPlatform().requireMinVersionValue("clSetCommandQueueProperty", 1.0, 1.1);
		error(CL.clSetCommandQueueProperty(getEntity(), property.value(), enabled ? CL_TRUE : CL_FALSE, 0));
	}
	

    @Override
    protected void clear() {
        error(CL.clReleaseCommandQueue(getEntity()));
    }

    /**
#documentCallsFunction("clFinish")
	 * Blocks until all previously queued OpenCL commands in this queue are issued to the associated device and have completed. <br/>
	 * finish() does not return until all queued commands in this queue have been processed and completed. <br/>
	 * finish() is also a synchronization point.
	 */
    public void finish() {
        error(CL.clFinish(getEntity()));
    }

    /**
#documentCallsFunction("clFlush")
	 * Issues all previously queued OpenCL commands in this queue to the device associated with this queue. <br/>
	 * flush() only guarantees that all queued commands in this queue get issued to the appropriate device. <br/>
	 * There is no guarantee that they will be complete after flush() returns.
	 */
    public void flush() {
        error(CL.clFlush(getEntity()));
    }

	/**
#documentCallsFunction("clEnqueueWaitForEvents")
	 * Enqueues a wait for a specific event or a list of events to complete before any future commands queued in the this queue are executed.
	 */
	public void enqueueWaitForEvents(CLEvent... eventsToWaitFor) {
		context.getPlatform().requireMinVersionValue("clEnqueueWaitForEvents", 1.1, 1.2);
		#declareReusablePtrs()
		#declareEventsIn()
        if (eventsIn == null)
            return;
        error(CL.clEnqueueWaitForEvents(getEntity(), #eventsInArgsRaw()));
	}
	

	/**
#documentCallsFunction("clEnqueueMigrateMemObjects")
	 * Enqueues a command to indicate which device a set of memory objects should be associated with.
	 */
	public CLEvent enqueueMigrateMemObjects(CLMem[] memObjects, EnumSet<CLMem.Migration> flags, CLEvent... eventsToWaitFor) {
		context.getPlatform().requireMinVersionValue("clEnqueueMigrateMemObjects", 1.2);
		#declareReusablePtrsAndEventsInOut()
		int n = 0;
		Pointer<SizeT> pMems = allocateSizeTs(memObjects.length);
		for (CLMem mem : memObjects) {
			if (mem != null) {
				pMems.setSizeTAtIndex(n++, mem.getEntity());
			}
		}
		error(CL.clEnqueueMigrateMemObjects(
			getEntity(),
			n,
			getPeer(pMems),
			CLMem.Migration.getValue(flags),
			#eventsInOutArgsRaw()
		));
		#returnEventOut("this")
	}

	/**
#documentCallsFunction("clEnqueueBarrierWithWaitList")
	 * Enqueue a barrier operation.<br/>
	 * The enqueueBarrier() command ensures that all queued commands in command_queue have finished execution before the next batch of commands can begin execution. <br/>
	 * enqueueBarrier() is a synchronization point.
#documentEventsToWaitForAndReturn()
	 */
	public CLEvent enqueueBarrier(CLEvent... eventsToWaitFor) {
		if (context.getPlatform().getVersionValue() >= 1.2 ||
			eventsToWaitFor != null && eventsToWaitFor.length > 0)
		{
			context.getPlatform().requireMinVersionValue("clEnqueueBarrierWithWaitList", 1.2);
			#declareReusablePtrsAndEventsInOut()
			error(CL.clEnqueueBarrierWithWaitList(
				getEntity(),
				#eventsInOutArgsRaw()
			));
			#returnEventOut("this")	
		} else {
			context.getPlatform().requireMinVersionValue("clEnqueueBarrier", 1.1, 1.2);
			error(CL.clEnqueueBarrier(getEntity()));
			return null;
		}
	}

	/**
#documentCallsFunction("clEnqueueMarkerWithWaitList")
	 * Enqueue a marker command to command_queue. <br/>
	 * The marker command returns an event which can be used by to queue a wait on this marker event i.e. wait for all commands queued before the marker command to complete.
#documentEventsToWaitForAndReturn()
	 */
	@Deprecated
	public CLEvent enqueueMarker(CLEvent... eventsToWaitFor) {
		if (context.getPlatform().getVersionValue() >= 1.2 ||
			eventsToWaitFor != null && eventsToWaitFor.length > 0)
		{
			context.getPlatform().requireMinVersionValue("clEnqueueMarkerWithWaitList", 1.2);
			#declareReusablePtrsAndEventsInOut()
	    	error(CL.clEnqueueMarkerWithWaitList(
				getEntity(),
				#eventsInOutArgsRaw()
			));
			#returnEventOut("this")
		} else {
			context.getPlatform().requireMinVersionValue("clEnqueueMarker", 1.1, 1.2);
			#declareReusablePtrs()
			Pointer<cl_event> eventOut = ptrs.event_out;
			error(CL.clEnqueueMarker(getEntity(), getPeer(eventOut)));
			#returnEventOut("this")
		}
	}

	/**
#documentCallsFunction("clEnqueueAcquireGLObjects")
	 * Used to acquire OpenCL memory objects that have been created from OpenGL objects. <br>
	 * These objects need to be acquired before they can be used by any OpenCL commands queued to a command-queue. <br>
	 * The OpenGL objects are acquired by the OpenCL context associated with this queue and can therefore be used by all command-queues associated with the OpenCL context.
	 * @param objects CL memory objects that correspond to GL objects.
#documentEventsToWaitForAndReturn()
	 */
	public CLEvent enqueueAcquireGLObjects(CLMem[] objects, CLEvent... eventsToWaitFor) {
        #declareReusablePtrsAndEventsInOut()
		Pointer<SizeT> mems = allocateSizeTs(objects.length);
		for (int i = 0; i < objects.length; i++) {
			mems.setSizeTAtIndex(i, objects[i].getEntity());
		}
        error(CL.clEnqueueAcquireGLObjects(
			getEntity(), 
			objects.length,
			getPeer(mems),
			#eventsInOutArgsRaw()
		));
		#returnEventOut("this")
	}

	/**
#documentCallsFunction("clEnqueueReleaseGLObjects")
	 * Used to release OpenCL memory objects that have been created from OpenGL objects. <br>
	 * These objects need to be released before they can be used by OpenGL. <br>
	 * The OpenGL objects are released by the OpenCL context associated with this queue.
	 * @param objects CL memory objects that correpond to GL objects.
#documentEventsToWaitForAndReturn()
	 */
	public CLEvent enqueueReleaseGLObjects(CLMem[] objects, CLEvent... eventsToWaitFor) {
        #declareReusablePtrsAndEventsInOut()
		Pointer<?> mems = getEntities(objects, (Pointer)allocateSizeTs(objects.length));
        error(CL.clEnqueueReleaseGLObjects(
			getEntity(), 
			objects.length, 
			getPeer(mems),
			#eventsInOutArgsRaw()
		));
		#returnEventOut("this")
	}
}
