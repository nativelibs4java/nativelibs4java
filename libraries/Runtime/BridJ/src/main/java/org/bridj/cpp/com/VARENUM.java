package org.bridj.cpp.com;
import java.util.Collections;
import java.util.Iterator;
import org.bridj.BridJ;
import org.bridj.FlagSet;
import org.bridj.IntValuedEnum;
import org.bridj.ValuedEnum;

public enum VARENUM implements IntValuedEnum<VARENUM > {
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
	VT_FILETIME(64),
	VT_BLOB(65),
	VT_STREAM(66),
	VT_STORAGE(67),
	VT_STREAMED_OBJECT(68),
	VT_STORED_OBJECT(69),
	VT_BLOB_OBJECT(70),
	VT_CF(71),
	VT_CLSID(72),
	VT_VECTOR(0x1000),
	VT_ARRAY(0x2000),
	VT_BYREF(0x4000),
	VT_RESERVED(0x8000),
	VT_ILLEGAL(0xFFFF),
	VT_ILLEGALMASKED(0xFFF),
	VT_TYPEMASK(0xFFF);
	VARENUM(long value) {
		this.value = value;
	}
	public final long value;
	public long value() {
		return this.value;
	}
	public Iterator<VARENUM > iterator() {
		return Collections.singleton(this).iterator();
	}
	public static ValuedEnum<VARENUM > fromValue(long value) {
		return FlagSet.fromValue(value, values());
	}
};
