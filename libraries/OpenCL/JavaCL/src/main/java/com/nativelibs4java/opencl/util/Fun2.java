/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.util;


public enum Fun2 {
    min,
    max,
    atan2,
    dist,
    modulo("%"),
    rshift(">>"),
    lshift("<<"),
    xor("^"),
    bitOr("|"),
    bitAnd("&"),
    add("+"),
    substract("-"),
    multiply("*"),
    divide("/");

    String infixOp;
    Fun2() {}
    Fun2(String infixOp) {
        this.infixOp = infixOp;
    }
    void expr(String a, String b, StringBuilder out) {
        if (infixOp == null)
            out.append(name()).append('(').append(a).append(", ").append(b).append(")");
        else
            out.append(a).append(' ').append(infixOp).append(' ').append(b);
    }
}