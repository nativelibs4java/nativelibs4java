/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nativelibs4java.jni;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.nativelibs4java.jni.ReflectionUtils.*;

/**
 *
 * @author ochafik
 */
public class ReflectionTest {
    public class Toto {
        private int value;
    }
    @Test
    public void testPrivateFields() throws NoSuchFieldException {
        NativeClass c = getNativeClass(Toto.class);
        NativeField<Integer> valueField = c.getField("value", int.class, false);
        
        Toto instance = new Toto();
        for (int value : new int[] { 1, 2, 0 }) {
            valueField.set(instance, value);
            assertEquals(value, (int)valueField.get(instance));
        }
    }
}
