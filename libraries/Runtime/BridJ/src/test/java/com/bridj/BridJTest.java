package com.bridj;


import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Set;

import org.junit.Test;
import static org.junit.Assert.*;

public class BridJTest {
	@Test
	public void symbolsTest() throws Exception {
		NativeLibrary lib = BridJ.getNativeLibrary("test");
		Collection<Demangler.Symbol> symbols = lib.getSymbols();
		
		assertTrue("Not enough symbols : found only " + symbols.size(), symbols.size() > 20);
		boolean found = false;
		for (Demangler.Symbol symbol : symbols) {
			if (symbol.getName().contains("Ctest")) {
				found = true;
				break;
			}
		}
		assertTrue("Failed to find any Ctest-related symbol !", found);
	}
}
