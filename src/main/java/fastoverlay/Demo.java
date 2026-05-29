package fastoverlay;

import java.awt.Color;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.Toolkit;

public class Demo {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting FastOverlay Demo...");
        FastOverlay.initEngine();
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        
        int screenW = screenSize.width;
        int screenH = screenSize.height;
        
        int hudW = 400;
        int hudH = 150;
        
        // Make the HWND fullscreen so DirectComposition can move the visual anywhere without clipping
        FastOverlayWindow overlay = new FastOverlayWindow(0, 0, screenW, screenH, true, true);
        
        boolean[] isGhostMode = {true};
        
        // Initial Draw (Only draw the HUD part)
        Runnable updateGraphics = () -> {
            overlay.setPainter(g -> {
                g.setColor(new Color(0, 0, 0, 0));
                g.fillRect(0, 0, screenW, screenH);
                
                // Draw HUD Background
                if (isGhostMode[0]) {
                    g.setColor(new Color(0, 200, 100, 180)); // Green for Ghost
                } else {
                    g.setColor(new Color(200, 50, 50, 180)); // Red for Solid
                }
                g.fillRoundRect(0, 0, hudW, hudH, 30, 30);
                
                // Draw Text
                g.setColor(Color.WHITE);
                g.setFont(new Font("Segoe UI", Font.BOLD, 24));
                g.drawString("FastOverlay v0.1.0", 90, 45);
                
                g.setFont(new Font("Segoe UI", Font.PLAIN, 18));
                if (isGhostMode[0]) {
                    g.drawString("Ghost Mode: ON (Click Through!)", 60, 90);
                } else {
                    g.drawString("Ghost Mode: OFF (Block Clicks!)", 70, 90);
                }
                
                g.setFont(new Font("Segoe UI", Font.ITALIC, 14));
                g.drawString("Toggling every 4 seconds...", 110, 120);
            });
        };
        
        updateGraphics.run();
        overlay.show();
        
        int ticks = 0;
        int centerX = (screenW - hudW) / 2;
        int centerY = (screenH - hudH) / 2;
        
        while (true) {
            // Smooth GPU movement without touching Win32 Window Pos
            // Base offset is center of screen (animation 5x slower)
            int offsetX = centerX + (int) (Math.sin(ticks * 0.006) * 300);
            int offsetY = centerY + (int) (Math.cos(ticks * 0.008) * 150);
            overlay.setVisualOffset(offsetX, offsetY);
            
            // Toggle Click-Through state every 4 seconds (approx 240 frames @ 60fps)
            if (ticks % 240 == 0 && ticks > 0) {
                isGhostMode[0] = !isGhostMode[0];
                overlay.setClickThrough(isGhostMode[0]);
                updateGraphics.run(); // Update the text and color
            }
            
            ticks++;
            Thread.sleep(16); // Target 60 FPS
        }
    }
}
