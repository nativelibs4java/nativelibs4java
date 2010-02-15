package com.bridj.ann;

import java.lang.annotation.ElementType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import static com.bridj.Dyncall.CallingConvention.*;
/**
 *
 * @author Olivier Chafik
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD, ElementType.PACKAGE, ElementType.PARAMETER, ElementType.CONSTRUCTOR})
public @interface CallingConvention {
    public enum Style {
        Auto,
        StdCall,
        //ThisCall,
        FastCall,
        CDecl
    }
    Style value() default Style.Auto;
}
