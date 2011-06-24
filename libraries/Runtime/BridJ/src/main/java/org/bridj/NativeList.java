/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bridj;

import java.util.RandomAccess;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import static org.bridj.Pointer.*;

/**
 *
 * @author ochafik
 */
public interface NativeList<T> extends List<T> {
    public Pointer<?> getPointer();
}
