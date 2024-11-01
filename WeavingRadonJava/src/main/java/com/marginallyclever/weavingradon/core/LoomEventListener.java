package com.marginallyclever.weavingradon.core;

import java.util.EventListener;

/**
    * An event listener for the Loom class.
 */
public interface LoomEventListener extends EventListener {
    void threadAdded(LoomThread thread);
}
