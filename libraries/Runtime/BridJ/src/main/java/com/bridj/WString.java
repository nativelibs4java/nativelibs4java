package com.bridj;

public final class WString {
	private final String string;
	public WString(Pointer<?> p) {
		this(p.getCString(0, true));
	}
	public WString(String string) {
		this.string = string;
	}
	@Override
	public String toString() {
		return string;
	}
}
