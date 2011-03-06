package org.bridj.cpp.com;
import org.bridj.*;
import org.bridj.ann.*;
import static org.bridj.cpp.com.VARIANT.VARENUM.*;
import static org.bridj.Pointer.*;
import java.util.Iterator;
import java.util.Collections;

/**
 * Value of any type (see http://msdn.microsoft.com/en-us/library/ms221627.aspx)
 */
public class VARIANT extends StructObject {
	@Field(0)
	public ValuedEnum<VARENUM> vt() {
        return io.getEnumField(this, 0);
    }
	public VARIANT vt(FlagSet<VARENUM> vt) {
        io.setEnumField(this, 0, vt);
        return this;
    }
	
	public enum VARENUM implements IntValuedEnum<VARENUM>
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
		public long value() {
			return value;
		}
		public Iterator<VARENUM> iterator() {
			return Collections.singleton(this).iterator();
		}
		public static ValuedEnum<VARENUM> fromValue(long value) {
			return FlagSet.fromValue(value, values());
		}
    };
    
    public static class SAFEARRAY extends StructObject {
    	/// Count of dimensions in this array.
    	@Field(0)
    	public native short cDims();
    	/// Flags used by the SafeArray
    	@Field(1)
    	public native short fFeatures();   
					
    	/**
    	 * Size of an element of the array.
         * Does not include size of
         * pointed-to data.
         */
		@Field(2)
    	public native int cbElements();  
		/**
		 * Number of times the array has been
		 * locked without corresponding unlock.
		 */
		@Field(3)
    	public native int cLocks();
		/// Pointer to the data.
		@Field(4)
    	public native Pointer<?> pvData();
		
		public static class SAFEARRAYBOUND extends StructObject {
		   @Field(0)
		   public native int cElements();
		   @Field(1)
		   public native int lLbound();
		}
		/// One bound for each dimension.
		@Field(5)
    	@Array(1)
		public native Pointer<SAFEARRAYBOUND> rgsabound();      
	}
    
	@Field(1)
	public native short wReserved1();
	@Field(2)
	public native short wReserved2();
	@Field(3)
	public native short wReserved3();
	@Field(4)
	public native long llVal();
	@Field(value = 5, unionWith = 4)
	public native int lVal();
	@Field(value = 6, unionWith = 4)
	public native byte bVal();
	@Field(value = 7, unionWith = 4)
	public native short iVal();
	@Field(value = 8, unionWith = 4)
	public native float fltVal();
	@Field(value = 9, unionWith = 4)
	public native double dblVal();
	@Field(value = 10, unionWith = 4)
	public native Pointer<Byte> bstrVal();
	@Field(value = 11, unionWith = 4)
	public native Pointer<Pointer<Byte>> pbstrVal();
	@Field(value = 12, unionWith = 4)
	public native Pointer<IUnknown> punkVal();
	@Field(value = 13, unionWith = 4)
	public native Pointer<IDispatch> pdispVal();
	@Field(value = 14, unionWith = 4)
	public native Pointer<Byte> pbVal();
	@Field(value = 15, unionWith = 4)
	public native Pointer<Short> piVal();
	@Field(value = 16, unionWith = 4)
	public native Pointer<Integer> plVal();
	@Field(value = 17, unionWith = 4)
	public native Pointer<Long> pllVal();
	@Field(value = 18, unionWith = 4)
	public native Pointer<Float> pfltVal();
	@Field(value = 19, unionWith = 4)
	public native Pointer<Double> pdblVal();
	@Field(value = 20, unionWith = 4)
	public native Pointer<Pointer<IUnknown>> ppunkVal();
	@Field(value = 21, unionWith = 4)
	public native Pointer<Pointer<IDispatch>> ppdispVal();
	@Field(value = 22, unionWith = 4)
	public native Pointer<VARIANT> pvarVal();
	@Field(value = 23, unionWith = 4)
	public native Pointer<?> byref();
	@Field(value = 24, unionWith = 4)
	public native short uiVal();
	@Field(value = 25, unionWith = 4)
	public native int ulVal();
	@Field(value = 26, unionWith = 4)
	public native long ullVal();
	@Field(value = 27, unionWith = 4)
	public native int intVal();
	@Field(value = 28, unionWith = 4)
	public native int uintVal();
	@Field(value = 29, unionWith = 4)
	public native Pointer<Short> puiVal();
	@Field(value = 30, unionWith = 4)
	public native Pointer<Integer> pulVal();
	@Field(value = 31, unionWith = 4)
	public native Pointer<Long> pullVal();
	@Field(value = 32, unionWith = 4)
	public native Pointer<Integer> pintVal();
	@Field(value = 33, unionWith = 4)
	public native Pointer<Integer> puintVal();
	
	@Field(34)
	public native BRECORD __VARIANT_NAME_3();
	
	public static class BRECORD extends StructObject {
		@Field(0)
		public native Pointer<?> pvRecord();
		@Field(1)
		public native Pointer<?>/*IRecordInfo*/ pRecInfo();
	}
	
	@Field(35)
	public native DECIMAL decVal();
	
	public static class DECIMAL extends StructObject {
		public DECIMAL() {
			super();
		}
		public DECIMAL(Pointer pointer) {
			super(pointer);
		}
		/// C type : USHORT
		@Field(0) 
		public short wReserved() {
			return this.io.getShortField(this, 0);
		}
		/// C type : USHORT
		@Field(0) 
		public DECIMAL wReserved(short wReserved) {
			this.io.setShortField(this, 0, wReserved);
			return this;
		}
		/// C type : USHORT
		public final short wReserved_$eq(short wReserved) {
			wReserved(wReserved);
			return wReserved;
		}
		/**
		 * The high 32 bits of your number<br>
		 * C type : ULONG
		 */
		@Field(1) 
		public int Hi32() {
			return this.io.getIntField(this, 1);
		}
		/**
		 * The high 32 bits of your number<br>
		 * C type : ULONG
		 */
		@Field(1) 
		public DECIMAL Hi32(int Hi32) {
			this.io.setIntField(this, 1, Hi32);
			return this;
		}
		/// C type : ULONG
		public final int Hi32_$eq(int Hi32) {
			Hi32(Hi32);
			return Hi32;
		}
	};
	
	/**
	 * Convert the VARIANT value to an equivalent Java value.
	 * @throws UnsupportedOperationException if the VARIANT type is not handled yet
	 * @throws RuntimeException if the VARIANT is invalid
	 */
	public Object getValue() {
		FlagSet<VARENUM> vt = FlagSet.fromValue(vt());
		if (vt.has(VT_BYREF)) {
			switch (vt.without(VT_BYREF).toEnum()) {
				case VT_DISPATCH:
					return ppdispVal();
				case VT_UNKNOWN:
					return ppunkVal();
				case VT_VARIANT:
					return pvarVal();
				case VT_I1       : 	
				case VT_UI1      : 	
					return pbVal();
				/* UINT16        */              
				case VT_I2      : 	
				case VT_UI2      : 	
					return piVal();
				/* UINT32        */              
				case VT_I4      : 	
				case VT_UI4      : 	
					return plVal();
				case VT_R4:
					return pfltVal();
				case VT_R8:
					return pdblVal();
				/* UINT64        */              
				case VT_I8      : 	
				case VT_UI8      : 	
					return pllVal();
				/* BOOL          */                    
				case VT_BOOL     : 	
					return pbVal().as(Boolean.class);
					
				case VT_BSTR:
					return pbstrVal();
				case VT_LPSTR:
					return byref().getCString();
				case VT_LPWSTR:
					return byref().getWideCString();
				case VT_PTR:
				default:
					return byref();
			}
		}
		switch (vt.toEnum()) {
			/* UINT8         */                    
			case VT_I1       : 	
			case VT_UI1      : 	
				return bVal();
			/* UINT16        */              
			case VT_I2      : 	
			case VT_UI2      : 	
				return uiVal();
			/* UINT32        */              
			case VT_I4      : 	
			case VT_UI4      : 	
				return ulVal();
			/* UINT64        */              
			case VT_I8      : 	
			case VT_UI8      : 	
				return ullVal();
			/* BOOL          */                    
			case VT_BOOL     : 	
				return bVal() != 0;
			case VT_R4:
				return fltVal();
			case VT_R8:
				return dblVal();
			case VT_BSTR:
				return bstrVal().getString(StringType.BSTR);
			case VT_INT_PTR:
			case VT_UINT_PTR:
				return piVal();
			default:
				throw new UnsupportedOperationException("Conversion not implemented yet from VARIANT type " + vt + " to Java !");
		}
	}
}
