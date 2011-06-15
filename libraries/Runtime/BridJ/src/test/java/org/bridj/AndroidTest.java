package org.bridj;


import org.bridj.demangling.Demangler;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Set;

import org.junit.Test;


import static org.junit.Assert.*;

public class AndroidTest {
	
	@Test
	public void testPackageNameExtraction() {
		String expected = "com.example.hellojni";
		for (String url : new String[] {
			"jar:file:/mnt/asec/com.example.hellojni-1/pkg.apk!/lib/armeabi/libhello-jni.so",
			"jar:file:/data/app/com.example.hellojni-1.apk!/lib/armeabi/libhello-jni.so"
			
		})
			assertEquals("URL not recognized : " + url, expected, BridJ.getAndroidPackageNameFromResourceURL(url));
	}
}
