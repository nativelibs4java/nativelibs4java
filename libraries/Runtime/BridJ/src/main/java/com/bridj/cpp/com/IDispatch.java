/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj.cpp.com;

import com.bridj.FlagSet;
import com.bridj.Pointer;
import com.bridj.StructObject;
import com.bridj.ann.Field;
import com.bridj.ann.Virtual;

/**
 *
 * @author Olivier
 */
@IID("00020400-0000-0000-C000-000000000046")
public class IDispatch extends IUnknown {
	public static class DISPPARAMS extends StructObject {
		@Field(0)
		public native Pointer<VARIANT> rgvarg();
		@Field(1)
		public native Pointer<Integer> rgdispidNamedArgs();
		@Field(2)
		public native int cArgs();
        @Field(3)
		public native int cNamedArgs();
	}
	
	public enum VARENUM
    {	
    	VT_EMPTY(0),
		VT_NULL(1),
		VT_I2(2),
		VT_I4(3),
		VT_R4(4),
		VT_R8(5),
		VT_CY(6),
		VT_DATE(7),
		VT_BSTR(8),
		VT_DISPATCH(9),
		VT_ERROR(10),
		VT_BOOL(11),
		VT_VARIANT(12),
		VT_UNKNOWN(13),
		VT_DECIMAL(14),
		VT_I1(16),
		VT_UI1(17),
		VT_UI2(18),
		VT_UI4(19),
		VT_I8(20),
		VT_UI8(21),
		VT_INT(22),
		VT_UINT(23),
		VT_VOID(24),
		VT_HRESULT(25),
		VT_PTR(26),
		VT_SAFEARRAY(27),
		VT_CARRAY(28),
		VT_USERDEFINED(29),
		VT_LPSTR(30),
		VT_LPWSTR(31),
		VT_RECORD(36),
		VT_INT_PTR(37),
		VT_UINT_PTR(38),
		VT_FILETIME(64),
		VT_BLOB(65),
		VT_STREAM(66),
		VT_STORAGE(67),
		VT_STREAMED_OBJECT(68),
		VT_STORED_OBJECT(69),
		VT_BLOB_OBJECT(70),
		VT_CF(71),
		VT_CLSID(72),
		VT_VERSIONED_STREAM(73),
		VT_BSTR_BLOB(0xfff),
		VT_VECTOR(0x1000),
		VT_ARRAY(0x2000),
		VT_BYREF(0x4000),
		VT_RESERVED(0x8000),
		VT_ILLEGAL(0xffff),
		VT_ILLEGALMASKED(0xfff),
		VT_TYPEMASK(0xfff);
		
		VARENUM(int value) {
			this.value = value;
		}
		int value;
    } ;
	public static class VARIANT extends StructObject {
		@Field(0)
		public native FlagSet<VARENUM> vt();
		public native void vt(FlagSet<VARENUM> vt);
		
		@Field(1)
		public native short wReserved1();
		@Field(2)
		public native short wReserved2();
		@Field(3)
		public native short wReserved3();
		@Field(4)
		public native long llVal();
		@Field(4)
		public native int lVal();
		@Field(4)
		public native byte bVal();
		@Field(4)
		public native short iVal();
		@Field(4)
		public native float fltVal();
		@Field(4)
		public native double dblVal();
		@Field(4)
		public native Pointer<IUnknown> punkVal();
		@Field(4)
		public native Pointer<IDispatch> pdispVal();
		@Field(4)
		public native Pointer<Byte> pbVal();
		@Field(4)
		public native Pointer<Short> piVal();
		@Field(4)
		public native Pointer<Integer> plVal();
		@Field(4)
		public native Pointer<Long> pllVal();
		@Field(4)
		public native Pointer<Float> pfltVal();
		@Field(4)
		public native Pointer<Double> pdblVal();
		@Field(4)
		public native Pointer<Pointer<IUnknown>> ppunkVal();
		@Field(4)
		public native Pointer<Pointer<IDispatch>> ppdispVal();
		@Field(4)
		public native Pointer<VARIANT> pvarVal();
		@Field(4)
		public native Pointer<?> byref();
		@Field(4)
		public native short uiVal();
		@Field(4)
		public native int ulVal();
		@Field(4)
		public native long ullVal();
		@Field(4)
		public native int intVal();
		@Field(4)
		public native int uintVal();
		@Field(4)
		public native Pointer<Short> puiVal();
		@Field(4)
		public native Pointer<Integer> pulVal();
		@Field(4)
		public native Pointer<Long> pullVal();
		@Field(4)
		public native Pointer<Integer> pintVal();
		@Field(4)
		public native Pointer<Integer> puintVal();
	}
	public static class EXCEPINFO extends StructObject {
		@Field(0)
		public native short wCode();
		@Field(1)
		public native short wReserved();
		@Field(2)
		public native Pointer<Character> bstrSource();
		@Field(3)
		public native Pointer<Character> bstrDescription();
		@Field(4)
		public native Pointer<Character> bstrHelpFile();
		@Field(5)
		public native int dwHelpContext();
		@Field(6)
		public native Pointer<?> pvReserved();
		@Field(7)
		public native Pointer<?> pfnDeferredFillIn();//HRESULT (__stdcall *pfnDeferredFillIn)(struct tagEXCEPINFO *);
		@Field(8)
		public native int scode();
	}
	@Virtual(0)
	public native int GetTypeInfoCount(Pointer<Integer> pctinfo);

	@Virtual(1)
	public native int GetTypeInfo(int iTInfo, int lcid, Pointer<Pointer<ITypeInfo>> ppTInfo);

	@Virtual(2) 
	public native int GetIDsOfNames(
		Pointer riid,//REFIID riid,
		Pointer<Pointer<Character>> rgszNames,
		int cNames,
		int lcid, //LCID lcid,
		Pointer<Integer> rgDispId); //DISPID *rgDispId);

	@Virtual(3)
	public native int Invoke(
		int dispIdMember,
		Pointer<Byte> riid,
		int lcid,
		short wFlags,
		Pointer<DISPPARAMS> pDispParams,
		Pointer<VARIANT> pVarResult,
		Pointer<EXCEPINFO> pExcepInfo,
		Pointer<Integer> puArgErr
	);
}
