package com.nativelibs4java.opencl.library;
import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Array;
import org.bridj.ann.Field;
import org.bridj.ann.Library;
import org.bridj.ann.Union;
/**
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> or <a href="http://bridj.googlecode.com/">BridJ</a> .
 */
@Union 
@Library("OpenCL") 
public class cl_char16 extends StructObject {
	public cl_char16() {
		super();
	}
	/// C type : cl_char[16]
	@Array({16}) 
	@Field(0) 
	public Pointer<Byte > s() {
		return this.io.getPointerField(this, 0);
	}
	/// C type : field1_struct
	@Field(1) 
	public com.nativelibs4java.opencl.library.cl_char2.field1_struct field1() {
		return this.io.getNativeObjectField(this, 1);
	}
	/// C type : field1_struct
	@Field(1) 
	public cl_char16 field1(com.nativelibs4java.opencl.library.cl_char2.field1_struct field1) {
		this.io.setNativeObjectField(this, 1, field1);
		return this;
	}
	/// C type : field2_struct
	@Field(2) 
	public com.nativelibs4java.opencl.library.cl_char2.field2_struct field2() {
		return this.io.getNativeObjectField(this, 2);
	}
	/// C type : field2_struct
	@Field(2) 
	public cl_char16 field2(com.nativelibs4java.opencl.library.cl_char2.field2_struct field2) {
		this.io.setNativeObjectField(this, 2, field2);
		return this;
	}
	/// C type : field3_struct
	@Field(3) 
	public com.nativelibs4java.opencl.library.cl_char2.field3_struct field3() {
		return this.io.getNativeObjectField(this, 3);
	}
	/// C type : field3_struct
	@Field(3) 
	public cl_char16 field3(com.nativelibs4java.opencl.library.cl_char2.field3_struct field3) {
		this.io.setNativeObjectField(this, 3, field3);
		return this;
	}
	public static class field1_struct extends StructObject {
		public field1_struct() {
			super();
		}
		/// C type : cl_char
		@Field(0) 
		public byte x() {
			return this.io.getByteField(this, 0);
		}
		/// C type : cl_char
		@Field(0) 
		public field1_struct x(byte x) {
			this.io.setByteField(this, 0, x);
			return this;
		}
		/// C type : cl_char
		@Field(1) 
		public byte y() {
			return this.io.getByteField(this, 1);
		}
		/// C type : cl_char
		@Field(1) 
		public field1_struct y(byte y) {
			this.io.setByteField(this, 1, y);
			return this;
		}
		/// C type : cl_char
		@Field(2) 
		public byte z() {
			return this.io.getByteField(this, 2);
		}
		/// C type : cl_char
		@Field(2) 
		public field1_struct z(byte z) {
			this.io.setByteField(this, 2, z);
			return this;
		}
		/// C type : cl_char
		@Field(3) 
		public byte w() {
			return this.io.getByteField(this, 3);
		}
		/// C type : cl_char
		@Field(3) 
		public field1_struct w(byte w) {
			this.io.setByteField(this, 3, w);
			return this;
		}
		/// C type : cl_char
		@Field(4) 
		public byte __spacer4() {
			return this.io.getByteField(this, 4);
		}
		/// C type : cl_char
		@Field(4) 
		public field1_struct __spacer4(byte __spacer4) {
			this.io.setByteField(this, 4, __spacer4);
			return this;
		}
		/// C type : cl_char
		@Field(5) 
		public byte __spacer5() {
			return this.io.getByteField(this, 5);
		}
		/// C type : cl_char
		@Field(5) 
		public field1_struct __spacer5(byte __spacer5) {
			this.io.setByteField(this, 5, __spacer5);
			return this;
		}
		/// C type : cl_char
		@Field(6) 
		public byte __spacer6() {
			return this.io.getByteField(this, 6);
		}
		/// C type : cl_char
		@Field(6) 
		public field1_struct __spacer6(byte __spacer6) {
			this.io.setByteField(this, 6, __spacer6);
			return this;
		}
		/// C type : cl_char
		@Field(7) 
		public byte __spacer7() {
			return this.io.getByteField(this, 7);
		}
		/// C type : cl_char
		@Field(7) 
		public field1_struct __spacer7(byte __spacer7) {
			this.io.setByteField(this, 7, __spacer7);
			return this;
		}
		/// C type : cl_char
		@Field(8) 
		public byte __spacer8() {
			return this.io.getByteField(this, 8);
		}
		/// C type : cl_char
		@Field(8) 
		public field1_struct __spacer8(byte __spacer8) {
			this.io.setByteField(this, 8, __spacer8);
			return this;
		}
		/// C type : cl_char
		@Field(9) 
		public byte __spacer9() {
			return this.io.getByteField(this, 9);
		}
		/// C type : cl_char
		@Field(9) 
		public field1_struct __spacer9(byte __spacer9) {
			this.io.setByteField(this, 9, __spacer9);
			return this;
		}
		/// C type : cl_char
		@Field(10) 
		public byte sa() {
			return this.io.getByteField(this, 10);
		}
		/// C type : cl_char
		@Field(10) 
		public field1_struct sa(byte sa) {
			this.io.setByteField(this, 10, sa);
			return this;
		}
		/// C type : cl_char
		@Field(11) 
		public byte sb() {
			return this.io.getByteField(this, 11);
		}
		/// C type : cl_char
		@Field(11) 
		public field1_struct sb(byte sb) {
			this.io.setByteField(this, 11, sb);
			return this;
		}
		/// C type : cl_char
		@Field(12) 
		public byte sc() {
			return this.io.getByteField(this, 12);
		}
		/// C type : cl_char
		@Field(12) 
		public field1_struct sc(byte sc) {
			this.io.setByteField(this, 12, sc);
			return this;
		}
		/// C type : cl_char
		@Field(13) 
		public byte sd() {
			return this.io.getByteField(this, 13);
		}
		/// C type : cl_char
		@Field(13) 
		public field1_struct sd(byte sd) {
			this.io.setByteField(this, 13, sd);
			return this;
		}
		/// C type : cl_char
		@Field(14) 
		public byte se() {
			return this.io.getByteField(this, 14);
		}
		/// C type : cl_char
		@Field(14) 
		public field1_struct se(byte se) {
			this.io.setByteField(this, 14, se);
			return this;
		}
		/// C type : cl_char
		@Field(15) 
		public byte sf() {
			return this.io.getByteField(this, 15);
		}
		/// C type : cl_char
		@Field(15) 
		public field1_struct sf(byte sf) {
			this.io.setByteField(this, 15, sf);
			return this;
		}
		public field1_struct(Pointer pointer) {
			super(pointer);
		}
	};
	public static class field2_struct extends StructObject {
		public field2_struct() {
			super();
		}
		/// C type : cl_char
		@Field(0) 
		public byte s0() {
			return this.io.getByteField(this, 0);
		}
		/// C type : cl_char
		@Field(0) 
		public field2_struct s0(byte s0) {
			this.io.setByteField(this, 0, s0);
			return this;
		}
		/// C type : cl_char
		@Field(1) 
		public byte s1() {
			return this.io.getByteField(this, 1);
		}
		/// C type : cl_char
		@Field(1) 
		public field2_struct s1(byte s1) {
			this.io.setByteField(this, 1, s1);
			return this;
		}
		/// C type : cl_char
		@Field(2) 
		public byte s2() {
			return this.io.getByteField(this, 2);
		}
		/// C type : cl_char
		@Field(2) 
		public field2_struct s2(byte s2) {
			this.io.setByteField(this, 2, s2);
			return this;
		}
		/// C type : cl_char
		@Field(3) 
		public byte s3() {
			return this.io.getByteField(this, 3);
		}
		/// C type : cl_char
		@Field(3) 
		public field2_struct s3(byte s3) {
			this.io.setByteField(this, 3, s3);
			return this;
		}
		/// C type : cl_char
		@Field(4) 
		public byte s4() {
			return this.io.getByteField(this, 4);
		}
		/// C type : cl_char
		@Field(4) 
		public field2_struct s4(byte s4) {
			this.io.setByteField(this, 4, s4);
			return this;
		}
		/// C type : cl_char
		@Field(5) 
		public byte s5() {
			return this.io.getByteField(this, 5);
		}
		/// C type : cl_char
		@Field(5) 
		public field2_struct s5(byte s5) {
			this.io.setByteField(this, 5, s5);
			return this;
		}
		/// C type : cl_char
		@Field(6) 
		public byte s6() {
			return this.io.getByteField(this, 6);
		}
		/// C type : cl_char
		@Field(6) 
		public field2_struct s6(byte s6) {
			this.io.setByteField(this, 6, s6);
			return this;
		}
		/// C type : cl_char
		@Field(7) 
		public byte s7() {
			return this.io.getByteField(this, 7);
		}
		/// C type : cl_char
		@Field(7) 
		public field2_struct s7(byte s7) {
			this.io.setByteField(this, 7, s7);
			return this;
		}
		/// C type : cl_char
		@Field(8) 
		public byte s8() {
			return this.io.getByteField(this, 8);
		}
		/// C type : cl_char
		@Field(8) 
		public field2_struct s8(byte s8) {
			this.io.setByteField(this, 8, s8);
			return this;
		}
		/// C type : cl_char
		@Field(9) 
		public byte s9() {
			return this.io.getByteField(this, 9);
		}
		/// C type : cl_char
		@Field(9) 
		public field2_struct s9(byte s9) {
			this.io.setByteField(this, 9, s9);
			return this;
		}
		/// C type : cl_char
		@Field(10) 
		public byte sA() {
			return this.io.getByteField(this, 10);
		}
		/// C type : cl_char
		@Field(10) 
		public field2_struct sA(byte sA) {
			this.io.setByteField(this, 10, sA);
			return this;
		}
		/// C type : cl_char
		@Field(11) 
		public byte sB() {
			return this.io.getByteField(this, 11);
		}
		/// C type : cl_char
		@Field(11) 
		public field2_struct sB(byte sB) {
			this.io.setByteField(this, 11, sB);
			return this;
		}
		/// C type : cl_char
		@Field(12) 
		public byte sC() {
			return this.io.getByteField(this, 12);
		}
		/// C type : cl_char
		@Field(12) 
		public field2_struct sC(byte sC) {
			this.io.setByteField(this, 12, sC);
			return this;
		}
		/// C type : cl_char
		@Field(13) 
		public byte sD() {
			return this.io.getByteField(this, 13);
		}
		/// C type : cl_char
		@Field(13) 
		public field2_struct sD(byte sD) {
			this.io.setByteField(this, 13, sD);
			return this;
		}
		/// C type : cl_char
		@Field(14) 
		public byte sE() {
			return this.io.getByteField(this, 14);
		}
		/// C type : cl_char
		@Field(14) 
		public field2_struct sE(byte sE) {
			this.io.setByteField(this, 14, sE);
			return this;
		}
		/// C type : cl_char
		@Field(15) 
		public byte sF() {
			return this.io.getByteField(this, 15);
		}
		/// C type : cl_char
		@Field(15) 
		public field2_struct sF(byte sF) {
			this.io.setByteField(this, 15, sF);
			return this;
		}
		public field2_struct(Pointer pointer) {
			super(pointer);
		}
	};
	public static class field3_struct extends StructObject {
		public field3_struct() {
			super();
		}
		/// C type : cl_char8
		@Field(0) 
		public cl_char8 lo() {
			return this.io.getNativeObjectField(this, 0);
		}
		/// C type : cl_char8
		@Field(0) 
		public field3_struct lo(cl_char8 lo) {
			this.io.setNativeObjectField(this, 0, lo);
			return this;
		}
		/// C type : cl_char8
		@Field(1) 
		public cl_char8 hi() {
			return this.io.getNativeObjectField(this, 1);
		}
		/// C type : cl_char8
		@Field(1) 
		public field3_struct hi(cl_char8 hi) {
			this.io.setNativeObjectField(this, 1, hi);
			return this;
		}
		public field3_struct(Pointer pointer) {
			super(pointer);
		}
	};
	public cl_char16(Pointer pointer) {
		super(pointer);
	}
}