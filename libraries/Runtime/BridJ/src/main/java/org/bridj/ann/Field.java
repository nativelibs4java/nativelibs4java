package org.bridj.ann;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate the index of a structure field (in Java, the order of methods and fields is unspecified so you need to order them explicitely).<br>
 * For C++ structs, the index is relative to the struct's declared class, not to the parent structures/classes (unlike {@link Virtual}, which virtual table offset is absolute).
 * @author ochafik
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Field {
    /**
     * Index of the field in a struct (first field has index 0).<br>
     * Fields of parent structures must be taken into account (if parent struct has 2 fields, first field of sub-struct has index 2).<br>
     * If more than one field are given the same index, this will produce an union at that index.
     */
    int value();

}
