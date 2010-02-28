package com.bridj;
import org.junit.Test;

import com.bridj.ann.*;

@Library("test")
@com.bridj.ann.Runtime(CRuntime.class)
public class DummyTest {
	
	@Test
	public void dummy() {
		BridJ.register();
	}
}
