package com.nativelibs4java.directx.d3d10;
import com.nativelibs4java.directx.d3d10.D3d10Library.D3D10_DSV_DIMENSION;
import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ValuedEnum;
import org.bridj.ann.Field;
import org.bridj.ann.Library;
/**
 * <i>native declaration : d3d10.h:683</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.free.fr/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> or <a href="http://bridj.googlecode.com/">BridJ</a> .
 */
@Library("d3d10") 
public class D3D10_DEPTH_STENCIL_VIEW_DESC extends StructObject {
	public D3D10_DEPTH_STENCIL_VIEW_DESC() {
		super();
	}
	public D3D10_DEPTH_STENCIL_VIEW_DESC(Pointer pointer) {
		super(pointer);
	}
	/// Conversion Error : DXGI_FORMAT (Unsupported type)
	/// C type : D3D10_DSV_DIMENSION
	@Field(0) 
	public ValuedEnum<D3D10_DSV_DIMENSION > ViewDimension() {
		return this.io.getEnumField(this, 0);
	}
	/// C type : D3D10_DSV_DIMENSION
	@Field(0) 
	public D3D10_DEPTH_STENCIL_VIEW_DESC ViewDimension(ValuedEnum<D3D10_DSV_DIMENSION > ViewDimension) {
		this.io.setEnumField(this, 0, ViewDimension);
		return this;
	}
}
