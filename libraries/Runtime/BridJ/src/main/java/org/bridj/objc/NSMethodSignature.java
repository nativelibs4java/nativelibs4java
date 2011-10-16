package org.bridj.objc;
import org.bridj.*;
import org.bridj.Pointer.StringType;
import org.bridj.ann.Library;
import java.nio.charset.*;
import static org.bridj.objc.FoundationLibrary.*;

@Library("Foundation")
public class NSMethodSignature extends NSObject {
	static { BridJ.register(); }

	public static native Pointer<NSMethodSignature> signatureWithObjCTypes(Pointer<Byte> types);	
}
