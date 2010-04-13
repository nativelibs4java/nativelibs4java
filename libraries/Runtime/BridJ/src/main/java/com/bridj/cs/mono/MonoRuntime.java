/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj.cs.mono;

import com.bridj.AbstractBridJRuntime;
import com.bridj.BridJ;
import com.bridj.NativeLibrary;
import com.bridj.NativeObject;
import com.bridj.Pointer;
import com.bridj.ann.Library;
import com.bridj.cs.CSharpRuntime;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Olivier
 */
@Library("mono")
public class MonoRuntime extends AbstractBridJRuntime implements CSharpRuntime {

    public MonoRuntime() {
        try {
            BridJ.register();
        } catch (Exception ex) {
            // Accept failure
            log(Level.INFO, "Failed to register " + getClass().getName(), ex);
        }
    }

    @Override
    public boolean isAvailable() {
        return getMonoLibrary() != null;
    }

    @Override
    public <T extends NativeObject> Class<? extends T> getActualInstanceClass(Pointer<T> pInstance, Class<T> officialType) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void register(Class<?> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    NativeLibrary monoLibrary;
    boolean fetchedLibrary;
    private synchronized NativeLibrary getMonoLibrary() {
        if (!fetchedLibrary && monoLibrary == null) {
            try {
                fetchedLibrary = true;
                monoLibrary = BridJ.getNativeLibrary("mono");
            } catch (FileNotFoundException ex) {
                log(Level.INFO, null, ex);
            }
        }
        return monoLibrary;
    }

    @Override
    public void initialize(NativeObject instance) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void initialize(NativeObject instance, Pointer peer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void initialize(NativeObject instance, int constructorId, Object[] args) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void destroy(NativeObject instance) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
