/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.runtime;

/**
 *
 * @author Olivier
 */
public class CPPObject implements Addressable {
    private long address;

    public CPPObject(Void v) {}

    public long getAddress() {
        return address;
    }
    public void setAddress(long address) {
        this.address = address;
    }
}
