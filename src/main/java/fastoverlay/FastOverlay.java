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

    // Native engine lifecycle
    public static native void initEngine();
    public static native void disposeEngine();

    // Native window management
    static native long createWindow(int x, int y, int width, int height, boolean transparent, boolean topmost);
    static native long createChildWindow(java.awt.Component component, int x, int y, int width, int height);
    static native void destroyWindow(long windowId);
    static native void setWindowPosition(long windowId, int x, int y);
    static native void setWindowSize(long windowId, int width, int height);
    public static native void setWindowProperties(long windowId, boolean alwaysOnTop, boolean clickThrough);
    public static native void updateWindowBitmap(long windowId, int[] pixels, int width, int height);
    public static native void setVisualOffset(long windowId, int x, int y);
    static native void setWindowVisible(long windowId, boolean visible);

}
