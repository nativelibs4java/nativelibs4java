/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj;

/**
 * @author ochafik
 */
public final class TempPointers {
    private final boolean refresh;
    private final Pointer<?>[] pointers;
    private long[] tempPeers;

    public TempPointers(Pointer<?>... pointers) {
        this(false, pointers);
    }
    public TempPointers(boolean refresh, Pointer<?>... pointers) {
        this.pointers = pointers;
        this.refresh = refresh;
        int len = pointers.length;
        tempPeers = new long[len];
        for (int i = 0; i < len; i++) {
            Pointer<?> p = pointers[i];
            if (p == null)
                continue;

            tempPeers[i] = p.getOrAllocateTempPeer();
        }
    }

    public long get(int index) {
        return tempPeers[index];
    }
    
    @Override
    protected void finalize() throws Throwable {
        if (tempPeers == null)
            return;

        release();
    }

    public void release() {
        if (tempPeers == null)
            throw new RuntimeException("Already released !");

        for (int i = 0, len = pointers.length; i < len; i++) {
            long peer = tempPeers[i];
            if (peer == 0)
                continue;
            Pointer<?> p = pointers[i];
            if (p == null)
                continue;

            p.deleteTempPeer(peer, refresh);
        }
    }
}
