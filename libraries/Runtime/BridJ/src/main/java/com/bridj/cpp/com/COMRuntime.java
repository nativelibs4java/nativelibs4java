package com.bridj.cpp.com;

import com.bridj.BridJ;
import com.bridj.Pointer;
import com.bridj.CRuntime;
import com.bridj.ann.Convention;
import com.bridj.ann.Library;
import com.bridj.ann.Ptr;
import com.bridj.ann.Runtime;
import com.bridj.cpp.CPPRuntime;

/**
 * Adding Icons, Previews and Shortcut Menus :
 * http://msdn.microsoft.com/en-us/library/bb266530(VS.85).aspx
 * 
 * TODO CoCreateInstanceEx
 * TODO CoRegisterClassObject
 * TODO CoRevokeClassObject
 * TODO CoCreateGuid 
 * 
 * IDL syntax : 
 * http://caml.inria.fr/pub/old_caml_site/camlidl/htmlman/main002.html
 * 
 * Registering a Running EXE Server :
 * http://msdn.microsoft.com/en-us/library/ms680076(VS.85).aspx
 */
@Library("Ole32")
@Runtime(CRuntime.class)
@Convention(Convention.Style.StdCall)
public class COMRuntime extends CPPRuntime {
	static {
		BridJ.register();
	}
	public static final int 
	  CLSCTX_INPROC_SERVER            = 0x1,
	  CLSCTX_INPROC_HANDLER           = 0x2,
	  CLSCTX_LOCAL_SERVER             = 0x4,
	  CLSCTX_INPROC_SERVER16          = 0x8,
	  CLSCTX_REMOTE_SERVER            = 0x10,
	  CLSCTX_INPROC_HANDLER16         = 0x20,
	  CLSCTX_RESERVED1                = 0x40,
	  CLSCTX_RESERVED2                = 0x80,
	  CLSCTX_RESERVED3                = 0x100,
	  CLSCTX_RESERVED4                = 0x200,
	  CLSCTX_NO_CODE_DOWNLOAD         = 0x400,
	  CLSCTX_RESERVED5                = 0x800,
	  CLSCTX_NO_CUSTOM_MARSHAL        = 0x1000,
	  CLSCTX_ENABLE_CODE_DOWNLOAD     = 0x2000,
	  CLSCTX_NO_FAILURE_LOG           = 0x4000,
	  CLSCTX_DISABLE_AAA              = 0x8000,
	  CLSCTX_ENABLE_AAA               = 0x10000,
	  CLSCTX_FROM_DEFAULT_CONTEXT     = 0x20000,
	  CLSCTX_ACTIVATE_32_BIT_SERVER   = 0x40000,
	  CLSCTX_ACTIVATE_64_BIT_SERVER   = 0x80000,
	  CLSCTX_ENABLE_CLOAKING          = 0x100000,
	  CLSCTX_PS_DLL                   = 0x80000000;

    public static final int
        CLSCTX_INPROC           = CLSCTX_INPROC_SERVER|CLSCTX_INPROC_HANDLER,
        CLSCTX_ALL              =(CLSCTX_INPROC_SERVER|
                                 CLSCTX_INPROC_HANDLER|
                                 CLSCTX_LOCAL_SERVER|
                                 CLSCTX_REMOTE_SERVER),
     CLSCTX_SERVER           = (CLSCTX_INPROC_SERVER|CLSCTX_LOCAL_SERVER|CLSCTX_REMOTE_SERVER);
    
	public static final int S_OK = 0,
            REGDB_E_CLASSNOTREG = 0x80040154,
            CLASS_E_NOAGGREGATION = 0x80040110,
            CO_E_NOTINITIALIZED = 0x800401F0;

    public static final int E_UNEXPECTED                    = 0x8000FFFF;
    public static final int E_NOTIMPL                       = 0x80004001;
    public static final int E_OUTOFMEMORY                   = 0x8007000E;
    public static final int E_INVALIDARG                    = 0x80070057;
    public static final int E_NOINTERFACE                   = 0x80004002;
    public static final int E_POINTER                       = 0x80004003;
    public static final int E_HANDLE                        = 0x80070006;
    public static final int E_ABORT                         = 0x80004004;
    public static final int E_FAIL                          = 0x80004005;
    public static final int E_ACCESSDENIED                  = 0x80070005;

    public static interface COINIT {
        public final int
            COINIT_APARTMENTTHREADED  = 0x2,      // Apartment model
            COINIT_MULTITHREADED      = 0x0,      // OLE calls objects on any thread.
            COINIT_DISABLE_OLE1DDE    = 0x4,      // Don't use DDE for Ole1 support.
            COINIT_SPEED_OVER_MEMORY  = 0x8;
    }
	@Deprecated
	public static native int CoCreateInstance(
		Pointer<Byte> rclsid,
		Pointer<IUnknown> pUnkOuter,
		int dwClsContext,
		Pointer<Byte> riid,
		Pointer<Pointer<?>> ppv
	);

    static native int CoInitializeEx(@Ptr long pvReserved, int dwCoInit);
    static native int CoInitialize(@Ptr long pvReserved);
    static native void CoUninitialize();

    static void error(int err) {
        switch (err) {
            case E_INVALIDARG:
            case E_OUTOFMEMORY:
            case E_UNEXPECTED:
                throw new RuntimeException("Error " + Integer.toHexString(err));
            case S_OK:
                return;
            case CO_E_NOTINITIALIZED:
                throw new RuntimeException("CoInitialized wasn't called !!");
            case E_NOINTERFACE:
                throw new RuntimeException("Interface does not inherit from class");
            case E_POINTER:
                throw new RuntimeException("Allocated pointer pointer is null !!");
            default:
                throw new RuntimeException("Unexpected COM error code : " + err);
        }
    }
	public static <I extends IUnknown> Pointer<Byte> getIID(Class<I> type) {
		IID id = type.getAnnotation(IID.class);
		if (id == null)
			throw new RuntimeException("No " + IID.class.getName() + " annotation set on type " + type.getName() + " !");

        return (Pointer)GUID.parseGUID128Bits(id.value());
	}
	
	public static <I extends IUnknown> Pointer<Byte> getCLSID(Class<I> type) {
		CLSID id = type.getAnnotation(CLSID.class);
		if (id == null)
			throw new RuntimeException("No " + CLSID.class.getName() + " annotation set on type " + type.getName() + " !");
        
		return (Pointer)GUID.parseGUID128Bits(id.value());
	}
    static ThreadLocal<Object> comInitializer = new ThreadLocal<Object>() {
        @Override
        protected Object initialValue() {
            error(CoInitializeEx(0, COINIT.COINIT_MULTITHREADED));
            return new Object() {
                @Override
                protected void finalize() throws Throwable {
                    CoUninitialize();
                }
            };
        }
    };
    
    /**
     * Initialize COM the current thread (uninitialization is done automatically upon thread death)
     * Calls CoInitialize with COINIT_MULTITHREADED max once per thread.
     */
    public static void initialize() {
        comInitializer.get();
    }
	public static <I extends IUnknown> I newInstance(Class<I> type) throws ClassNotFoundException {
        return newInstance(type, type);
    }
    public static <T extends IUnknown, I extends IUnknown> I newInstance(Class<T> instanceClass, Class<I> instanceInterface) throws ClassNotFoundException {
        initialize();
        
		Pointer<Pointer<?>> p = Pointer.allocatePointer();
        Pointer<Byte> clsid = getCLSID(instanceClass), uuid = getIID(instanceInterface);
        try {
            int ret = CoCreateInstance(clsid, null, CLSCTX_ALL, uuid, p);
            if (ret == REGDB_E_CLASSNOTREG)
                throw new ClassNotFoundException("COM class is not registered : " + instanceClass.getSimpleName() + " (clsid = " + clsid.getCString(0) + ")");
            error(ret);

            Pointer<?> inst = p.getPointer(0);
            if (inst == null)
                throw new RuntimeException("Serious low-level issue : CoCreateInstance executed fine but we only retrieved a null pointer !");

            I instance = inst.toNativeObject(instanceInterface);
            return instance;
        } finally {
            Pointer.release(p, clsid, uuid);
        }
	}
}
