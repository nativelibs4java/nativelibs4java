/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl;

/**
 *
 * @author ochafik
 */
public class CLException extends RuntimeException {
    final int code;
    public CLException(String message, int code) {
        super(message);
        this.code = code;
    }
    public int getCode() {
        return code;
    }
}