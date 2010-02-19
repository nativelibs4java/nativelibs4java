/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj.mfc;

import com.bridj.Struct;
import com.bridj.ann.Field;

/**
 *
 * @author Olivier
 */
public class CPoint extends Struct<CPoint> {
    @Field(0)
    public native int x();
    @Field(0)
    public native void x(int x);
    @Field(0)
    public native int y();
    @Field(0)
    public native void y(int y);
}
