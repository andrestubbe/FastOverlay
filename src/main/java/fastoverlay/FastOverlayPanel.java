package fastoverlay;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Window;
import java.awt.Point;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.function.Consumer;

public class FastOverlayPanel extends JComponent {

    private long windowId = 0;
    private BufferedImage bufferImage;
    private Graphics bufferGraphics;
    private boolean isDirty = true;
    private Consumer<java.awt.Graphics2D> painter;

    public FastOverlayPanel() {
        setOpaque(false);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        
        Window parentWindow = SwingUtilities.windowForComponent(this);
        if (parentWindow != null) {
            Point p = SwingUtilities.convertPoint(this, 0, 0, parentWindow);
            windowId = FastOverlay.createChildWindow(parentWindow, p.x, p.y, getWidth(), getHeight());
            if (windowId != 0) {
                FastOverlay.setWindowVisible(windowId, true);
                if (isDirty && painter != null) {
                    forceRepaint();
                }
            }
        }
    }

    @Override
    public void removeNotify() {
        if (windowId != 0) {
            FastOverlay.destroyWindow(windowId);
            windowId = 0;
        }
        super.removeNotify();
    }

    @Override
    public void setBounds(int x, int y, int w, int h) {
        super.setBounds(x, y, w, h);

        if (windowId != 0) {
            Window parentWindow = SwingUtilities.windowForComponent(this);
            if (parentWindow != null) {
                Point p = SwingUtilities.convertPoint(this, 0, 0, parentWindow);
                FastOverlay.setWindowPosition(windowId, p.x, p.y);
            }
            FastOverlay.setWindowSize(windowId, w, h);
            
            if (bufferImage == null || bufferImage.getWidth() != w || bufferImage.getHeight() != h) {
                bufferImage = null;
                bufferGraphics = null;
                isDirty = true;
            }
            
            forceRepaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Swing draws NOTHING here. 
        // Rendering is handled 100% by DirectComposition GPU backend.
    }
    
    public void setPainter(Consumer<java.awt.Graphics2D> painter) {
        this.painter = painter;
        this.isDirty = true;
        forceRepaint();
    }

    public void setVisualOffset(int x, int y) {
        if (windowId != 0) {
            FastOverlay.setVisualOffset(windowId, x, y);
        }
    }

    public void updateImage(BufferedImage img) {
        if (windowId != 0) {
            int[] pixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
            FastOverlay.updateWindowBitmap(windowId, pixels, img.getWidth(), img.getHeight());
        }
    }
    
    public void forceRepaint() {
        if (windowId == 0 || painter == null) return;
        
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;
        
        if (bufferImage == null) {
            bufferImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            bufferGraphics = bufferImage.getGraphics();
        }
        
        painter.accept((java.awt.Graphics2D) bufferGraphics);
        updateImage(bufferImage);
        isDirty = false;
    }
}
