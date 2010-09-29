/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.bridj.objc;
import org.bridj.Pointer;
import org.bridj.ann.Library;

@Library("Foundation")
public class NSString extends ObjCObject {
    public native int length();
    public native boolean isAbsolutePath();

    public NSString(Pointer<? extends NSString> peer) {
        super(peer);
    }

    public NSString() {
        super();
    }
}
