package com.marginallyclever.weavingradon.core;

/**
 * when new threads are added to the loom, this event is fired.
 */
public class LoomEvent {
    public final LoomThread thread;

    public LoomEvent(LoomThread thread) {
        this.thread = thread;
    }
}
