package com.bridj.cpp.mfc;

import com.bridj.Pointer;

import com.bridj.ann.Convention;
import com.bridj.ann.Virtual;
import com.bridj.ann.Convention.Style;

@Convention(Style.StdCall)
public class CObject extends MFCObject {

	public CObject() {}
	public CObject(Pointer<? extends CObject> pInstance, MFCRuntime mfcRuntime) {
		super(pInstance);
	}
	
	@Virtual
	public native Pointer<CRuntimeClass> GetRuntimeClass();

	/**
	 * {@link http://msdn.microsoft.com/en-us/library/b7tsah76(VS.80).aspx}
	 * @param pClass
	 * @return
	 */
	@Virtual
	public native boolean IsKindOf(Pointer<CRuntimeClass> pClass); 
}
