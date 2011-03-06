package org.bridj.cpp.com;
import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ValuedEnum;
import org.bridj.ann.CLong;
import org.bridj.ann.Field;
import org.bridj.ann.Library;
import org.bridj.ann.Union;
import org.bridj.ann.Runtime;

/**
 * Represents an object that can be interpreted as more than one type.
 */
@Runtime(COMRuntime.class)
public class VARIANT extends StructObject {
	public VARIANT(Object value) {
		super();
        COMRuntime.setValue(this, value);
	}
	public VARIANT() {
		super();
	}
	public VARIANT(Pointer pointer) {
		super(pointer);
	}
	/// C type : __VARIANT_NAME_1_union
	@Field(0)
	public VARIANT.__VARIANT_NAME_1_union __VARIANT_NAME_1() {
		return this.io.getNativeObjectField(this, 0);
	}
	/// <i>native declaration : line 107</i>
	@Union
	public static class __VARIANT_NAME_1_union extends StructObject {
		public __VARIANT_NAME_1_union() {
			super();
		}
		public __VARIANT_NAME_1_union(Pointer pointer) {
			super(pointer);
		}
		/// C type : __tagVARIANT
		@Field(0)
		public VARIANT.__VARIANT_NAME_1_union.__tagVARIANT __VARIANT_NAME_2() {
			return this.io.getNativeObjectField(this, 0);
		}
		/// C type : DECIMAL
		@Field(0)
		public DECIMAL decVal() {
			return this.io.getNativeObjectField(this, 0);
		}
		/// <i>native declaration : line 109</i>
		public static class __tagVARIANT extends StructObject {
			public __tagVARIANT() {
				super();
			}
			public __tagVARIANT(Pointer pointer) {
				super(pointer);
			}
			/// C type : VARTYPE
			@Field(0)
			public ValuedEnum<VARENUM > vt() {
				return this.io.getEnumField(this, 0);
			}
			/// C type : VARTYPE
			@Field(0)
			public __tagVARIANT vt(ValuedEnum<VARENUM > vt) {
				this.io.setEnumField(this, 0, vt);
				return this;
			}
			/// C type : VARTYPE
			public final ValuedEnum<VARENUM > vt_$eq(ValuedEnum<VARENUM > vt) {
				vt(vt);
				return vt;
			}
			@Field(1)
			public short wReserved1() {
				return this.io.getShortField(this, 1);
			}
			@Field(1)
			public __tagVARIANT wReserved1(short wReserved1) {
				this.io.setShortField(this, 1, wReserved1);
				return this;
			}
			public final short wReserved1_$eq(short wReserved1) {
				wReserved1(wReserved1);
				return wReserved1;
			}
			@Field(2)
			public short wReserved2() {
				return this.io.getShortField(this, 2);
			}
			@Field(2)
			public __tagVARIANT wReserved2(short wReserved2) {
				this.io.setShortField(this, 2, wReserved2);
				return this;
			}
			public final short wReserved2_$eq(short wReserved2) {
				wReserved2(wReserved2);
				return wReserved2;
			}
			@Field(3)
			public short wReserved3() {
				return this.io.getShortField(this, 3);
			}
			@Field(3)
			public __tagVARIANT wReserved3(short wReserved3) {
				this.io.setShortField(this, 3, wReserved3);
				return this;
			}
			public final short wReserved3_$eq(short wReserved3) {
				wReserved3(wReserved3);
				return wReserved3;
			}
			/// C type : __VARIANT_NAME_3_union
			@Field(4)
			public VARIANT.__VARIANT_NAME_1_union.__tagVARIANT.__VARIANT_NAME_3_union __VARIANT_NAME_3() {
				return this.io.getNativeObjectField(this, 4);
			}
			/// <i>native declaration : line 115</i>
			@Union
			public static class __VARIANT_NAME_3_union extends StructObject {
				public __VARIANT_NAME_3_union() {
					super();
				}
				public __VARIANT_NAME_3_union(Pointer pointer) {
					super(pointer);
				}
				/// VT_I8
				@Field(0)
				public long llval() {
					return this.io.getLongField(this, 0);
				}
				/// VT_I8
				@Field(0)
				public __VARIANT_NAME_3_union llval(long llval) {
					this.io.setLongField(this, 0, llval);
					return this;
				}
				public final long llval_$eq(long llval) {
					llval(llval);
					return llval;
				}
				/// VT_I4
				@CLong
				@Field(0)
				public long lVal() {
					return this.io.getCLongField(this, 0);
				}
				/// VT_I4
				@CLong
				@Field(0)
				public __VARIANT_NAME_3_union lVal(long lVal) {
					this.io.setCLongField(this, 0, lVal);
					return this;
				}
				public final long lVal_$eq(long lVal) {
					lVal(lVal);
					return lVal;
				}
				/// VT_UI1
				@Field(0)
				public byte bVal() {
					return this.io.getByteField(this, 0);
				}
				/// VT_UI1
				@Field(0)
				public __VARIANT_NAME_3_union bVal(byte bVal) {
					this.io.setByteField(this, 0, bVal);
					return this;
				}
				public final byte bVal_$eq(byte bVal) {
					bVal(bVal);
					return bVal;
				}
				/**
				 * VT_I2<br>
				 * C type : SHORT
				 */
				@Field(0)
				public short iVal() {
					return this.io.getShortField(this, 0);
				}
				/**
				 * VT_I2<br>
				 * C type : SHORT
				 */
				@Field(0)
				public __VARIANT_NAME_3_union iVal(short iVal) {
					this.io.setShortField(this, 0, iVal);
					return this;
				}
				/// C type : SHORT
				public final short iVal_$eq(short iVal) {
					iVal(iVal);
					return iVal;
				}
				/**
				 * VT_R4<br>
				 * C type : FLOAT
				 */
				@Field(0)
				public float fltVal() {
					return this.io.getFloatField(this, 0);
				}
				/**
				 * VT_R4<br>
				 * C type : FLOAT
				 */
				@Field(0)
				public __VARIANT_NAME_3_union fltVal(float fltVal) {
					this.io.setFloatField(this, 0, fltVal);
					return this;
				}
				/// C type : FLOAT
				public final float fltVal_$eq(float fltVal) {
					fltVal(fltVal);
					return fltVal;
				}
				/**
				 * VT_R8<br>
				 * C type : DOUBLE
				 */
				@Field(0)
				public double dblVal() {
					return this.io.getDoubleField(this, 0);
				}
				/**
				 * VT_R8<br>
				 * C type : DOUBLE
				 */
				@Field(0)
				public __VARIANT_NAME_3_union dblVal(double dblVal) {
					this.io.setDoubleField(this, 0, dblVal);
					return this;
				}
				/// C type : DOUBLE
				public final double dblVal_$eq(double dblVal) {
					dblVal(dblVal);
					return dblVal;
				}
				/**
				 * VT_BOOL<br>
				 * C type : VARIANT_BOOL
				 */
				@Field(0)
				public boolean boolVal() {
					return this.io.getBooleanField(this, 0);
				}
				/**
				 * VT_BOOL<br>
				 * C type : VARIANT_BOOL
				 */
				@Field(0)
				public __VARIANT_NAME_3_union boolVal(boolean boolVal) {
					this.io.setBooleanField(this, 0, boolVal);
					return this;
				}
				/// C type : VARIANT_BOOL
				public final boolean boolVal_$eq(boolean boolVal) {
					boolVal(boolVal);
					return boolVal;
				}
				/// C type : _VARIANT_BOOL
				@Field(0)
				public boolean bool() {
					return this.io.getBooleanField(this, 0);
				}
				/// C type : _VARIANT_BOOL
				@Field(0)
				public __VARIANT_NAME_3_union bool(boolean bool) {
					this.io.setBooleanField(this, 0, bool);
					return this;
				}
				/// C type : _VARIANT_BOOL
				public final boolean bool_$eq(boolean bool) {
					bool(bool);
					return bool;
				}
				/**
				 * VT_ERROR<br>
				 * C type : SCODE
				 */
				@Field(0)
				public int scode() {
					return this.io.getIntField(this, 0);
				}
				/**
				 * VT_ERROR<br>
				 * C type : SCODE
				 */
				@Field(0)
				public __VARIANT_NAME_3_union scode(int scode) {
					this.io.setIntField(this, 0, scode);
					return this;
				}
				/// C type : SCODE
				public final int scode_$eq(int scode) {
					scode(scode);
					return scode;
				}
				/**
				 * VT_CY<br>
				 * C type : CY
				 */
				@Field(0)
				public CY cyVal() {
					return this.io.getNativeObjectField(this, 0);
				}
				/**
				 * VT_DATE<br>
				 * C type : DATE
				 */
				@Field(0)
				public double date() {
					return this.io.getDoubleField(this, 0);
				}
				/**
				 * VT_DATE<br>
				 * C type : DATE
				 */
				@Field(0)
				public __VARIANT_NAME_3_union date(double date) {
					this.io.setDoubleField(this, 0, date);
					return this;
				}
				/// C type : DATE
				public final double date_$eq(double date) {
					date(date);
					return date;
				}
				/**
				 * VT_BSTR<br>
				 * C type : BSTR
				 */
				@Field(0)
				public Pointer<Byte > bstrVal() {
					return this.io.getPointerField(this, 0);
				}
				/**
				 * VT_BSTR<br>
				 * C type : BSTR
				 */
				@Field(0)
				public __VARIANT_NAME_3_union bstrVal(Pointer<Byte > bstrVal) {
					this.io.setPointerField(this, 0, bstrVal);
					return this;
				}
				/// C type : BSTR
				public final Pointer<Byte > bstrVal_$eq(Pointer<Byte > bstrVal) {
					bstrVal(bstrVal);
					return bstrVal;
				}
				/**
				 * VT_UNKNOWN<br>
				 * C type : IUnknown*
				 */
				@Field(0)
				public Pointer<IUnknown > punkVal() {
					return this.io.getPointerField(this, 0);
				}
				/**
				 * VT_UNKNOWN<br>
				 * C type : IUnknown*
				 */
				@Field(0)
				public __VARIANT_NAME_3_union punkVal(Pointer<IUnknown > punkVal) {
					this.io.setPointerField(this, 0, punkVal);
					return this;
				}
				/// C type : IUnknown*
				public final Pointer<IUnknown > punkVal_$eq(Pointer<IUnknown > punkVal) {
					punkVal(punkVal);
					return punkVal;
				}
				/**
				 * VT_DISPATCH<br>
				 * C type : IDispatch*
				 */
				@Field(0)
				public Pointer<IDispatch > pdispVal() {
					return this.io.getPointerField(this, 0);
				}
				/**
				 * VT_DISPATCH<br>
				 * C type : IDispatch*
				 */
				@Field(0)
				public __VARIANT_NAME_3_union pdispVal(Pointer<IDispatch > pdispVal) {
					this.io.setPointerField(this, 0, pdispVal);
					return this;
				}
				/// C type : IDispatch*
				public final Pointer<IDispatch > pdispVal_$eq(Pointer<IDispatch > pdispVal) {
					pdispVal(pdispVal);
					return pdispVal;
				}
				/**
				 * VT_ARRAY|*<br>
				 * C type : SAFEARRAY*
				 */
				@Field(0)
				public Pointer<SAFEARRAY > parray() {
					return this.io.getPointerField(this, 0);
				}
				/**
				 * VT_ARRAY|*<br>
				 * C type : SAFEARRAY*
				 */
				@Field(0)
				public __VARIANT_NAME_3_union parray(Pointer<SAFEARRAY > parray) {
					this.io.setPointerField(this, 0, parray);
					return this;
				}
				/// C type : SAFEARRAY*
				public final Pointer<SAFEARRAY > parray_$eq(Pointer<SAFEARRAY > parray) {
					parray(parray);
					return parray;
				}
				/**
				 * VT_BYREF|VT_UI1<br>
				 * C type : BYTE*
				 */
				@Field(0)
				public Pointer<Byte > pbVal() {
					return this.io.getPointerField(this, 0);
				}
				/**
				 * VT_BYREF|VT_UI1<br>
				 * C type : BYTE*
				 */
				@Field(0)
				public __VARIANT_NAME_3_union pbVal(Pointer<Byte > pbVal) {
					this.io.setPointerField(this, 0, pbVal);
					return this;
				}
				/// C type : BYTE*
				public final Pointer<Byte > pbVal_$eq(Pointer<Byte > pbVal) {
					pbVal(pbVal);
					return pbVal;
				}
				/**
				 * VT_BYREF|VT_I2<br>
				 * C type : SHORT*
				 */
				@Field(0)
				public Pointer<Short > piVal() {
					return this.io.getPointerField(this, 0);
				}
				/**
				 * VT_BYREF|VT_I2<br>
				 * C type : SHORT*
				 */
				@Field(0)
				public __VARIANT_NAME_3_union piVal(Pointer<Short > piVal) {
					this.io.setPointerField(this, 0, piVal);
					return this;
				}
				/// C type : SHORT*
				public final Pointer<Short > piVal_$eq(Pointer<Short > piVal) {
					piVal(piVal);
					return piVal;
				}
				/**
				 * VT_BYREF|VT_I4<br>
				 * C type : LONG*
				 */
				@Field(0)
				public Pointer<CLong > plVal() {
					return this.io.getPointerField(this, 0);
				}
				/**
				 * VT_BYREF|VT_I4<br>
				 * C type : LONG*
				 */
				@Field(0)
				public __VARIANT_NAME_3_union plVal(Pointer<CLong > plVal) {
					this.io.setPointerField(this, 0, plVal);
					return this;
				}
				/// C type : LONG*
				public final Pointer<CLong > plVal_$eq(Pointer<CLong > plVal) {
					plVal(plVal);
					return plVal;
				}
				/**
				 * VT_BYREF|VT_I8<br>
				 * C type : LONGLONG*
				 */
				@Field(0)
				public Pointer<Long > pllVal() {
					return this.io.getPointerField(this, 0);
				}
				/**
				 * VT_BYREF|VT_I8<br>
				 * C type : LONGLONG*
				 */
				@Field(0)
				public __VARIANT_NAME_3_union pllVal(Pointer<Long > pllVal) {
					this.io.setPointerField(this, 0, pllVal);
					return this;
				}
				/// C type : LONGLONG*
				public final Pointer<Long > pllVal_$eq(Pointer<Long > pllVal) {
					pllVal(pllVal);
					return pllVal;
				}
				/**
				 * VT_BYREF|VT_R4<br>
				 * C type : FLOAT*
				 */
				@Field(0)
				public Pointer<Float > pfltVal() {
					return this.io.getPointerField(this, 0);
				}
				/**
				 * VT_BYREF|VT_R4<br>
				 * C type : FLOAT*
				 */
				@Field(0)
				public __VARIANT_NAME_3_union pfltVal(Pointer<Float > pfltVal) {
					this.io.setPointerField(this, 0, pfltVal);
					return this;
				}
				/// C type : FLOAT*
				public final Pointer<Float > pfltVal_$eq(Pointer<Float > pfltVal) {
					pfltVal(pfltVal);
					return pfltVal;
				}
				/**
				 * VT_BYREF|VT_R8<br>
				 * C type : DOUBLE*
				 */
				@Field(0)
				public Pointer<Double > pdblVal() {
					return this.io.getPointerField(this, 0);
				}
				/**
				 * VT_BYREF|VT_R8<br>
				 * C type : DOUBLE*
				 */
				@Field(0)
				public __VARIANT_NAME_3_union pdblVal(Pointer<Double > pdblVal) {
					this.io.setPointerField(this, 0, pdblVal);
					return this;
				}
				/// C type : DOUBLE*
				public final Pointer<Double > pdblVal_$eq(Pointer<Double > pdblVal) {
					pdblVal(pdblVal);
					return pdblVal;
				}
				/**
				 * VT_BYREF|VT_BOOL<br>
				 * C type : VARIANT_BOOL*
				 */
				@Field(0)
				public Pointer<Boolean > pboolVal() {
					return this.io.getPointerField(this, 0);
				}
				/**
				 * VT_BYREF|VT_BOOL<br>
				 * C type : VARIANT_BOOL*
				 */
				@Field(0)
				public __VARIANT_NAME_3_union pboolVal(Pointer<Boolean > pboolVal) {
					this.io.setPointerField(this, 0, pboolVal);
					return this;
				}
				/// C type : VARIANT_BOOL*
				public final Pointer<Boolean > pboolVal_$eq(Pointer<Boolean > pboolVal) {
					pboolVal(pboolVal);
					return pboolVal;
				}
				/// C type : _VARIANT_BOOL*
				@Field(0)
				public Pointer<Boolean > pbool() {
					return this.io.getPointerField(this, 0);
				}
				/// C type : _VARIANT_BOOL*
				@Field(0)
				public __VARIANT_NAME_3_union pbool(Pointer<Boolean > pbool) {
					this.io.setPointerField(this, 0, pbool);
					return this;
				}
				/// C type : _VARIANT_BOOL*
				public final Pointer<Boolean > pbool_$eq(Pointer<Boolean > pbool) {
					pbool(pbool);
					return pbool;
				}
				/**
				 * VT_BYREF|VT_ERROR<br>
				 * C type : SCODE*
				 */
				@Field(0)
				public Pointer<Integer > pscode() {
					return this.io.getPointerField(this, 0);
				}
				/**
				 * VT_BYREF|VT_ERROR<br>
				 * C type : SCODE*
				 */
				@Field(0)
				public __VARIANT_NAME_3_union pscode(Pointer<Integer > pscode) {
					this.io.setPointerField(this, 0, pscode);
					return this;
				}
				/// C type : SCODE*
				public final Pointer<Integer > pscode_$eq(Pointer<Integer > pscode) {
					pscode(pscode);
					return pscode;
				}
				/**
				 * VT_BYREF|VT_CY<br>
				 * C type : CY*
				 */
				@Field(0)
				public Pointer<CY > pcyVal() {
					return this.io.getPointerField(this, 0);
				}
				/**
				 * VT_BYREF|VT_CY<br>
				 * C type : CY*
				 */
				@Field(0)
				public __VARIANT_NAME_3_union pcyVal(Pointer<CY > pcyVal) {
					this.io.setPointerField(this, 0, pcyVal);
					return this;
				}
				/// C type : CY*
				public final Pointer<CY > pcyVal_$eq(Pointer<CY > pcyVal) {
					pcyVal(pcyVal);
					return pcyVal;
				}
				/**
				 * VT_BYREF|VT_DATE<br>
				 * C type : DATE*
				 */
				@Field(0)
				public Pointer<Double > pdate() {
					return this.io.getPointerField(this, 0);
				}
				/**
				 * VT_BYREF|VT_DATE<br>
				 * C type : DATE*
				 */
				@Field(0)
				public __VARIANT_NAME_3_union pdate(Pointer<Double > pdate) {
					this.io.setPointerField(this, 0, pdate);
					return this;
				}
				/// C type : DATE*
				public final Pointer<Double > pdate_$eq(Pointer<Double > pdate) {
					pdate(pdate);
					return pdate;
				}
				/**
				 * VT_BYREF|VT_BSTR<br>
				 * C type : BSTR*
				 */
				@Field(0)
				public Pointer<Pointer<Byte > > pbstrVal() {
					return this.io.getPointerField(this, 0);
				}
				/**
				 * VT_BYREF|VT_BSTR<br>
				 * C type : BSTR*
				 */
				@Field(0)
				public __VARIANT_NAME_3_union pbstrVal(Pointer<Pointer<Byte > > pbstrVal) {
					this.io.setPointerField(this, 0, pbstrVal);
					return this;
				}
				/// C type : BSTR*
				public final Pointer<Pointer<Byte > > pbstrVal_$eq(Pointer<Pointer<Byte > > pbstrVal) {
					pbstrVal(pbstrVal);
					return pbstrVal;
				}
				/**
				 * VT_BYREF|VT_UNKNOWN<br>
				 * C type : IUnknown**
				 */
				@Field(0)
				public Pointer<Pointer<IUnknown > > ppunkVal() {
					return this.io.getPointerField(this, 0);
				}
				/**
				 * VT_BYREF|VT_UNKNOWN<br>
				 * C type : IUnknown**
				 */
				@Field(0)
				public __VARIANT_NAME_3_union ppunkVal(Pointer<Pointer<IUnknown > > ppunkVal) {
					this.io.setPointerField(this, 0, ppunkVal);
					return this;
				}
				/// C type : IUnknown**
				public final Pointer<Pointer<IUnknown > > ppunkVal_$eq(Pointer<Pointer<IUnknown > > ppunkVal) {
					ppunkVal(ppunkVal);
					return ppunkVal;
				}
				/**
				 * VT_BYREF|VT_DISPATCH<br>
				 * C type : IDispatch**
				 */
				@Field(0)
				public Pointer<Pointer<IDispatch > > ppdispVal() {
					return this.io.getPointerField(this, 0);
				}
				/**
				 * VT_BYREF|VT_DISPATCH<br>
				 * C type : IDispatch**
				 */
				@Field(0)
				public __VARIANT_NAME_3_union ppdispVal(Pointer<Pointer<IDispatch > > ppdispVal) {
					this.io.setPointerField(this, 0, ppdispVal);
					return this;
				}
				/// C type : IDispatch**
				public final Pointer<Pointer<IDispatch > > ppdispVal_$eq(Pointer<Pointer<IDispatch > > ppdispVal) {
					ppdispVal(ppdispVal);
					return ppdispVal;
				}
				/**
				 * VT_BYREF|VT_ARRAY<br>
				 * C type : SAFEARRAY**
				 */
				@Field(0)
				public Pointer<Pointer<SAFEARRAY > > pparray() {
					return this.io.getPointerField(this, 0);
				}
				/**
				 * VT_BYREF|VT_ARRAY<br>
				 * C type : SAFEARRAY**
				 */
				@Field(0)
				public __VARIANT_NAME_3_union pparray(Pointer<Pointer<SAFEARRAY > > pparray) {
					this.io.setPointerField(this, 0, pparray);
					return this;
				}
				/// C type : SAFEARRAY**
				public final Pointer<Pointer<SAFEARRAY > > pparray_$eq(Pointer<Pointer<SAFEARRAY > > pparray) {
					pparray(pparray);
					return pparray;
				}
				/**
				 * VT_BYREF|VT_VARIANT<br>
				 * C type : VARIANT*
				 */
				@Field(0)
				public Pointer<VARIANT > pvarVal() {
					return this.io.getPointerField(this, 0);
				}
				/**
				 * VT_BYREF|VT_VARIANT<br>
				 * C type : VARIANT*
				 */
				@Field(0)
				public __VARIANT_NAME_3_union pvarVal(Pointer<VARIANT > pvarVal) {
					this.io.setPointerField(this, 0, pvarVal);
					return this;
				}
				/// C type : VARIANT*
				public final Pointer<VARIANT > pvarVal_$eq(Pointer<VARIANT > pvarVal) {
					pvarVal(pvarVal);
					return pvarVal;
				}
				/**
				 * Generic ByRef<br>
				 * C type : PVOID*
				 */
				@Field(0)
				public Pointer<Pointer<? > > byref() {
					return this.io.getPointerField(this, 0);
				}
				/**
				 * Generic ByRef<br>
				 * C type : PVOID*
				 */
				@Field(0)
				public __VARIANT_NAME_3_union byref(Pointer<Pointer<? > > byref) {
					this.io.setPointerField(this, 0, byref);
					return this;
				}
				/// C type : PVOID*
				public final Pointer<Pointer<? > > byref_$eq(Pointer<Pointer<? > > byref) {
					byref(byref);
					return byref;
				}
				/**
				 * VT_I1<br>
				 * C type : CHAR
				 */
				@Field(0)
				public byte cVal() {
					return this.io.getByteField(this, 0);
				}
				/**
				 * VT_I1<br>
				 * C type : CHAR
				 */
				@Field(0)
				public __VARIANT_NAME_3_union cVal(byte cVal) {
					this.io.setByteField(this, 0, cVal);
					return this;
				}
				/// C type : CHAR
				public final byte cVal_$eq(byte cVal) {
					cVal(cVal);
					return cVal;
				}
				/**
				 * VT_UI2<br>
				 * C type : USHORT
				 */
				@Field(0)
				public short uiVal() {
					return this.io.getShortField(this, 0);
				}
				/**
				 * VT_UI2<br>
				 * C type : USHORT
				 */
				@Field(0)
				public __VARIANT_NAME_3_union uiVal(short uiVal) {
					this.io.setShortField(this, 0, uiVal);
					return this;
				}
				/// C type : USHORT
				public final short uiVal_$eq(short uiVal) {
					uiVal(uiVal);
					return uiVal;
				}
				/**
				 * VT_UI4<br>
				 * C type : ULONG
				 */
				@Field(0)
				public int ulVal() {
					return this.io.getIntField(this, 0);
				}
				/**
				 * VT_UI4<br>
				 * C type : ULONG
				 */
				@Field(0)
				public __VARIANT_NAME_3_union ulVal(int ulVal) {
					this.io.setIntField(this, 0, ulVal);
					return this;
				}
				/// C type : ULONG
				public final int ulVal_$eq(int ulVal) {
					ulVal(ulVal);
					return ulVal;
				}
				/// VT_UI8
				@Field(0)
				public long ullVal() {
					return this.io.getLongField(this, 0);
				}
				/// VT_UI8
				@Field(0)
				public __VARIANT_NAME_3_union ullVal(long ullVal) {
					this.io.setLongField(this, 0, ullVal);
					return this;
				}
				public final long ullVal_$eq(long ullVal) {
					ullVal(ullVal);
					return ullVal;
				}
				/**
				 * VT_INT<br>
				 * C type : INT
				 */
				@Field(0)
				public int intVal() {
					return this.io.getIntField(this, 0);
				}
				/**
				 * VT_INT<br>
				 * C type : INT
				 */
				@Field(0)
				public __VARIANT_NAME_3_union intVal(int intVal) {
					this.io.setIntField(this, 0, intVal);
					return this;
				}
				/// C type : INT
				public final int intVal_$eq(int intVal) {
					intVal(intVal);
					return intVal;
				}
				/**
				 * VT_UINT<br>
				 * C type : UINT
				 */
				@Field(0)
				public int uintVal() {
					return this.io.getIntField(this, 0);
				}
				/**
				 * VT_UINT<br>
				 * C type : UINT
				 */
				@Field(0)
				public __VARIANT_NAME_3_union uintVal(int uintVal) {
					this.io.setIntField(this, 0, uintVal);
					return this;
				}
				/// C type : UINT
				public final int uintVal_$eq(int uintVal) {
					uintVal(uintVal);
					return uintVal;
				}
				/**
				 * VT_BYREF|VT_DECIMAL<br>
				 * C type : DECIMAL*
				 */
				@Field(0)
				public Pointer<DECIMAL > pdecVal() {
					return this.io.getPointerField(this, 0);
				}
				/**
				 * VT_BYREF|VT_DECIMAL<br>
				 * C type : DECIMAL*
				 */
				@Field(0)
				public __VARIANT_NAME_3_union pdecVal(Pointer<DECIMAL > pdecVal) {
					this.io.setPointerField(this, 0, pdecVal);
					return this;
				}
				/// C type : DECIMAL*
				public final Pointer<DECIMAL > pdecVal_$eq(Pointer<DECIMAL > pdecVal) {
					pdecVal(pdecVal);
					return pdecVal;
				}
				/**
				 * VT_BYREF|VT_I1<br>
				 * C type : CHAR*
				 */
				@Field(0)
				public Pointer<Byte > pcVal() {
					return this.io.getPointerField(this, 0);
				}
				/**
				 * VT_BYREF|VT_I1<br>
				 * C type : CHAR*
				 */
				@Field(0)
				public __VARIANT_NAME_3_union pcVal(Pointer<Byte > pcVal) {
					this.io.setPointerField(this, 0, pcVal);
					return this;
				}
				/// C type : CHAR*
				public final Pointer<Byte > pcVal_$eq(Pointer<Byte > pcVal) {
					pcVal(pcVal);
					return pcVal;
				}
				/**
				 * VT_BYREF|VT_UI2<br>
				 * C type : USHORT*
				 */
				@Field(0)
				public Pointer<Short > puiVal() {
					return this.io.getPointerField(this, 0);
				}
				/**
				 * VT_BYREF|VT_UI2<br>
				 * C type : USHORT*
				 */
				@Field(0)
				public __VARIANT_NAME_3_union puiVal(Pointer<Short > puiVal) {
					this.io.setPointerField(this, 0, puiVal);
					return this;
				}
				/// C type : USHORT*
				public final Pointer<Short > puiVal_$eq(Pointer<Short > puiVal) {
					puiVal(puiVal);
					return puiVal;
				}
				/**
				 * VT_BYREF|VT_UI4<br>
				 * C type : ULONG*
				 */
				@Field(0)
				public Pointer<Integer > pulVal() {
					return this.io.getPointerField(this, 0);
				}
				/**
				 * VT_BYREF|VT_UI4<br>
				 * C type : ULONG*
				 */
				@Field(0)
				public __VARIANT_NAME_3_union pulVal(Pointer<Integer > pulVal) {
					this.io.setPointerField(this, 0, pulVal);
					return this;
				}
				/// C type : ULONG*
				public final Pointer<Integer > pulVal_$eq(Pointer<Integer > pulVal) {
					pulVal(pulVal);
					return pulVal;
				}
				/**
				 * VT_BYREF|VT_UI8<br>
				 * C type : ULONGLONG*
				 */
				@Field(0)
				public Pointer<Long > pullVal() {
					return this.io.getPointerField(this, 0);
				}
				/**
				 * VT_BYREF|VT_UI8<br>
				 * C type : ULONGLONG*
				 */
				@Field(0)
				public __VARIANT_NAME_3_union pullVal(Pointer<Long > pullVal) {
					this.io.setPointerField(this, 0, pullVal);
					return this;
				}
				/// C type : ULONGLONG*
				public final Pointer<Long > pullVal_$eq(Pointer<Long > pullVal) {
					pullVal(pullVal);
					return pullVal;
				}
				/**
				 * VT_BYREF|VT_INT<br>
				 * C type : INT*
				 */
				@Field(0)
				public Pointer<Integer > pintVal() {
					return this.io.getPointerField(this, 0);
				}
				/**
				 * VT_BYREF|VT_INT<br>
				 * C type : INT*
				 */
				@Field(0)
				public __VARIANT_NAME_3_union pintVal(Pointer<Integer > pintVal) {
					this.io.setPointerField(this, 0, pintVal);
					return this;
				}
				/// C type : INT*
				public final Pointer<Integer > pintVal_$eq(Pointer<Integer > pintVal) {
					pintVal(pintVal);
					return pintVal;
				}
				/**
				 * VT_BYREF|VT_UINT<br>
				 * C type : UINT*
				 */
				@Field(0)
				public Pointer<Integer > puintVal() {
					return this.io.getPointerField(this, 0);
				}
				/**
				 * VT_BYREF|VT_UINT<br>
				 * C type : UINT*
				 */
				@Field(0)
				public __VARIANT_NAME_3_union puintVal(Pointer<Integer > puintVal) {
					this.io.setPointerField(this, 0, puintVal);
					return this;
				}
				/// C type : UINT*
				public final Pointer<Integer > puintVal_$eq(Pointer<Integer > puintVal) {
					puintVal(puintVal);
					return puintVal;
				}
				/// C type : __tagBRECORD
				@Field(0)
				public VARIANT.__VARIANT_NAME_1_union.__tagVARIANT.__VARIANT_NAME_3_union.__tagBRECORD __VARIANT_NAME_4() {
					return this.io.getNativeObjectField(this, 0);
				}
				/// <i>native declaration : line 162</i>
				public static class __tagBRECORD extends StructObject {
					public __tagBRECORD() {
						super();
					}
					public __tagBRECORD(Pointer pointer) {
						super(pointer);
					}
					/// C type : PVOID
					@Field(0)
					public Pointer<? > pvRecord() {
						return this.io.getPointerField(this, 0);
					}
					/// C type : PVOID
					@Field(0)
					public __tagBRECORD pvRecord(Pointer<? > pvRecord) {
						this.io.setPointerField(this, 0, pvRecord);
						return this;
					}
					/// C type : PVOID
					public final Pointer<? > pvRecord_$eq(Pointer<? > pvRecord) {
						pvRecord(pvRecord);
						return pvRecord;
					}
					/// C type : IRecordInfo*
					@Field(1)
					public Pointer<IRecordInfo > pRecInfo() {
						return this.io.getPointerField(this, 1);
					}
					/// C type : IRecordInfo*
					@Field(1)
					public __tagBRECORD pRecInfo(Pointer<IRecordInfo > pRecInfo) {
						this.io.setPointerField(this, 1, pRecInfo);
						return this;
					}
					/// C type : IRecordInfo*
					public final Pointer<IRecordInfo > pRecInfo_$eq(Pointer<IRecordInfo > pRecInfo) {
						pRecInfo(pRecInfo);
						return pRecInfo;
					}
				};
			};
		};
	};
    public Object getValue() {
        return COMRuntime.getValue(this);
    }
    public VARIANT setValue(Object value) {
        return COMRuntime.setValue(this, value);
    }

    @Override
    public String toString() {
        return COMRuntime.toString(this);
    }


}
