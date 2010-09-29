/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.util;


public enum Fun1 {
    not("!"),
    complement("~"),
    abs,
    log,
    exp,
    sqrt,
    sin,
    cos,
    tan,
    atan,
    asin,
    acos,
    sinh,
    cosh,
    tanh,
    asinh,
    acosh,
    atanh;

    final String prefixOp;
    Fun1(String op) {
        this.prefixOp = op;
    }
    Fun1() {
        this(null);
    }
    void expr(String a, StringBuilder out) {
        if (prefixOp != null)
            out.append('(').append(prefixOp).append(a).append(')');
        out.append(name()).append('(').append(a).append(')');
    }
}