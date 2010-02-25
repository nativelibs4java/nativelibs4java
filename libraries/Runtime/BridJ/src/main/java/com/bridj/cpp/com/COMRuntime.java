package com.bridj.cpp.com;

import com.bridj.BridJ;
import com.bridj.Pointer;
import com.bridj.ann.Library;
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
 * Registering a Running EXE Server :
 * http://msdn.microsoft.com/en-us/library/ms680076(VS.85).aspx
 */
@Library("Ole32")
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
	  
	public static final int S_OK = 0;
	
	@Deprecated
	public static native int CoCreateInstance(
		Pointer<Byte> rclsid,
		Pointer<IUnknown> pUnkOuter,
		int dwClsContext,
		Pointer<Byte> riid,
		Pointer<Pointer<?>> ppv
	);
	
	public static <I extends IUnknown> Pointer<Byte> getUUID(Class<I> type) {
		UUID id = type.getAnnotation(UUID.class);
		if (id == null)
			throw new RuntimeException("No " + UUID.class.getName() + " annotation set on type " + type.getName() + " !");
		return Pointer.pointerTo(id.value());
	}
	
	public static <I extends IUnknown> Pointer<Byte> getCLSID(Class<I> type) {
		CLSID id = type.getAnnotation(CLSID.class);
		if (id == null)
			throw new RuntimeException("No " + CLSID.class.getName() + " annotation set on type " + type.getName() + " !");
		return Pointer.pointerTo(id.value());
	}
	
	public static <I extends IUnknown> I CoCreateInstance(Class<I> type) {
		Pointer<Pointer<?>> p = Pointer.allocatePointer();
		int flags = 0; // TODO
		int ret = CoCreateInstance(getCLSID(type), null, flags, getUUID(type), p);
		if (ret != S_OK)
			return null;
		
		return p.get().toNativeObject(type);
	}
}
