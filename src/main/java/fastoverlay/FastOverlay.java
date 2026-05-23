package fastoverlay;

import fastcore.FastCore;

/**
 * FastOverlay Main API Class.
 * Native Windows capabilities exposed via JNI.
 */
public class FastOverlay {

    // Load the native library once upon class initialization
    static {
        FastCore.loadLibrary("fastoverlay");
    }

    /**
     * Executes the native capability via C++.
     */
    public native void doSomethingNative();

}
