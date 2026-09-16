package fastoverlay;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;

/**
 * FastOverlay Showcase Demo:
 * 60 FPS GPU-accelerated DirectComposition floating HUD with dynamic click-through toggle.
 */
public class Demo {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting FastOverlay Demo...");
        FastOverlay.initEngine();
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenW = screenSize.width;
        int screenH = screenSize.height;
        
        int hudW = 420;
        int hudH = 160;
        
        // Fullscreen native overlay container for DirectComposition free-form movement
        FastOverlayWindow overlay = new FastOverlayWindow(0, 0, screenW, screenH, true, true);
        
        boolean[] isGhostMode = {true};
        
        // Render routine for the floating HUD
        Runnable updateGraphics = () -> {
            overlay.setPainter(g -> {
                g.setColor(new Color(0, 0, 0, 0));
                g.fillRect(0, 0, screenW, screenH);
                
                // HUD Background (semi-transparent rounded box)
                if (isGhostMode[0]) {
                    g.setColor(new Color(16, 185, 129, 210)); // Emerald Green for Ghost
                } else {
                    g.setColor(new Color(239, 68, 68, 210));  // Crimson Red for Solid
                }
                g.fillRoundRect(0, 0, hudW, hudH, 28, 28);
                
                // HUD Border
                g.setColor(new Color(255, 255, 255, 90));
                g.drawRoundRect(0, 0, hudW, hudH, 28, 28);
                
                // HUD Header & Metrics
                g.setColor(Color.WHITE);
                g.setFont(new Font("Segoe UI", Font.BOLD, 22));
                g.drawString("⚡ FastOverlay DirectComposition", 24, 42);
                
                g.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                if (isGhostMode[0]) {
                    g.drawString("Mode: Ghost (Click-Through Active)", 24, 80);
                } else {
                    g.drawString("Mode: Solid (Click Capturing Active)", 24, 80);
                }
                
                g.setFont(new Font("Segoe UI", Font.ITALIC, 13));
                g.drawString("DirectComposition Visual Translation @ 60 FPS", 24, 115);
                g.drawString("Toggling click-through every 4 seconds...", 24, 138);
            });
        };
        
        updateGraphics.run();
        overlay.show();
        
        int ticks = 0;
        int centerX = (screenW - hudW) / 2;
        int centerY = (screenH - hudH) / 2;
        
        while (true) {
            // Smooth hardware GPU translation without touching Win32 SetWindowPos
            int offsetX = centerX + (int) (Math.sin(ticks * 0.008) * 320);
            int offsetY = centerY + (int) (Math.cos(ticks * 0.010) * 160);
            overlay.setVisualOffset(offsetX, offsetY);
            
            // Toggle click-through state every 4 seconds (240 ticks @ 60 FPS)
            if (ticks % 240 == 0 && ticks > 0) {
                isGhostMode[0] = !isGhostMode[0];
                overlay.setClickThrough(isGhostMode[0]);
                updateGraphics.run();
            }
            
            ticks++;
            Thread.sleep(16); // ~60 FPS
        }
    }
}
