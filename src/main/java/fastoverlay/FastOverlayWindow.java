package fastoverlay;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * FastOverlayWindow represents a single, GPU-accelerated overlay window.
 * Backed by Direct2D, DirectComposition, and layered windows.
 */
public class FastOverlayWindow {

    private long windowId = 0;
    private int width;
    private int height;
    private boolean topmost;
    private boolean transparent;

    /**
     * Creates a new FastOverlayWindow.
     * 
     * @param x Initial X position
     * @param y Initial Y position
     * @param width Window width
     * @param height Window height
     * @param transparent If true, mouse events pass through the window (click-through). If false, it's clickable.
     * @param topmost If true, the window stays on top of other windows.
     */
    public FastOverlayWindow(int x, int y, int width, int height, boolean transparent, boolean topmost) {
        this.width = width;
        this.height = height;
        this.transparent = transparent;
        this.topmost = topmost;
        this.windowId = FastOverlay.createWindow(x, y, width, height, transparent, topmost);
    }

    /**
     * Dynamically toggles whether mouse clicks pass through the overlay.
     */
    public void setClickThrough(boolean clickThrough) {
        if (this.transparent == clickThrough) return;
        this.transparent = clickThrough;
        if (windowId != 0) {
            FastOverlay.setWindowProperties(windowId, this.topmost, this.transparent);
        }
    }

    /**
     * Dynamically toggles whether the overlay stays on top of other windows.
     */
    public void setTopmost(boolean topmost) {
        if (this.topmost == topmost) return;
        this.topmost = topmost;
        if (windowId != 0) {
            FastOverlay.setWindowProperties(windowId, this.topmost, this.transparent);
        }
    }

    private BufferedImage bufferImage;

    /**
     * Renders a Java2D drawing to the native Direct2D window.
     * 
     * @param painter The drawing routine
     */
    public void setPainter(Consumer<Graphics2D> painter) {
        if (windowId == 0) return;

        if (bufferImage == null || bufferImage.getWidth() != width || bufferImage.getHeight() != height) {
            bufferImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        } else {
            // clear it instantly using JVM intrinsics
            int[] pixels = ((java.awt.image.DataBufferInt) bufferImage.getRaster().getDataBuffer()).getData();
            java.util.Arrays.fill(pixels, 0);
        }

        Graphics2D g = bufferImage.createGraphics();
        
        // High quality defaults
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        painter.accept(g);
        g.dispose();
        
        dirty = true;
        updateImage(bufferImage);
    }

    private int[] premulBuffer;
    private boolean dirty = true;

    public void markDirty() {
        dirty = true;
    }

    /**
     * Updates the window's bitmap with a pre-rendered BufferedImage.
     * 
     * @param img The image to render
     */
    public void updateImage(BufferedImage img) {
        if (windowId == 0 || img == null) return;
        
        if (!dirty) return;
        dirty = false;

        int[] src = ((java.awt.image.DataBufferInt) img.getRaster().getDataBuffer()).getData();
        int len = src.length;
        if (premulBuffer == null || premulBuffer.length != len) {
            premulBuffer = new int[len];
        }
        
        for (int i = 0; i < len; i++) {
            int argb = src[i];
            int a = (argb >>> 24);
            if (a == 255) {
                // Fully opaque: swap R and B (ARGB -> BGRA-style premultiplied)
                premulBuffer[i] = argb;
            } else if (a == 0) {
                premulBuffer[i] = 0;
            } else {
                int r = ((argb >> 16) & 0xFF) * a / 255;
                int g = ((argb >>  8) & 0xFF) * a / 255;
                int b = ( argb        & 0xFF) * a / 255;
                premulBuffer[i] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        FastOverlay.updateWindowBitmap(windowId, premulBuffer, img.getWidth(), img.getHeight());
    }

    public void setPosition(int x, int y) {
        if (windowId != 0) {
            FastOverlay.setWindowPosition(windowId, x, y);
        }
    }

    public void setVisualOffset(int x, int y) {
        if (windowId != 0) {
            FastOverlay.setVisualOffset(windowId, x, y);
        }
    }

    public void setSize(int width, int height) {
        if (windowId != 0) {
            this.width = width;
            this.height = height;
            FastOverlay.setWindowSize(windowId, width, height);
        }
    }

    public void show() {
        if (windowId != 0) {
            FastOverlay.setWindowVisible(windowId, true);
        }
    }

    public void hide() {
        if (windowId != 0) {
            FastOverlay.setWindowVisible(windowId, false);
        }
    }

    public void dispose() {
        if (windowId != 0) {
            FastOverlay.destroyWindow(windowId);
            windowId = 0;
        }
    }
}
