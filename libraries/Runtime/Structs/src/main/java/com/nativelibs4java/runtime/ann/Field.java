/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.runtime.ann;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 *
 * @author ochafik
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Field {
    /**
     * If more than one field are given the same index, this will produce an union at that index.
     * @return
     */
    int index();

    int arraySize() default -1;
    
    /**
     * Zero forces alignment reset or something... (seen in the wild)
     * @return
     */
    int bits() default -1;

}
