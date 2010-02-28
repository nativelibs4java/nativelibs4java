/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj.ann;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.bridj.BridJRuntime;

/**
 *
 * @author Olivier
 */
@Target({ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Runtime {
    Class //<? extends BridJRuntime>
            value();
}
