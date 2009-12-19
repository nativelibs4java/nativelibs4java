/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.runtime;

/**
 *
 * @author Olivier Chafik
 */
public interface Addressable {
    long getAddress();
    void setAddress(long address);
}
