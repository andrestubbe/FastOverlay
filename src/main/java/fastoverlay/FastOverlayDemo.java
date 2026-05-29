package fastoverlay;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class FastOverlayDemo {
    public static void main(String[] args) throws InterruptedException {
        clearConsole();
        System.out.println("FastOverlay v0.1.0");
        System.out.println("──────────────────────────────────────────────");
        System.out.println();
        System.out.println("Initializing FastOverlay Engine...");
        FastOverlay.initEngine();

        System.out.println("Creating Agent HUD Window...");
        // A clickable, topmost window at (100, 100) size 400x300
        FastOverlayWindow hud = new FastOverlayWindow(100, 100, 400, 300, false, true);
        
        hud.show();
        System.out.println("HUD is visible. Watch the animation!");
        
        // Simple animation
        int progress = 0;
        while (true) {
            final int p = progress;
            hud.setPainter(g -> {
                g.setColor(new Color(30, 30, 40, 220));
                g.fillRoundRect(0, 0, 400, 300, 20, 20);
                
                g.setColor(new Color(255, 255, 255, 60));
                g.drawRoundRect(0, 0, 400, 300, 20, 20);

                g.setColor(Color.WHITE);
                g.setFont(new Font("Segoe UI", Font.BOLD, 24));
                g.drawString("FastOverlay Agent HUD", 20, 40);

                g.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                g.drawString("Status: Processing...", 20, 80);
                g.drawString("Memory: " + (140 + p % 20) + " MB", 20, 110);
                
                g.setColor(new Color(0, 0, 0, 100));
                g.fillRoundRect(20, 140, 360, 20, 10, 10);
                g.setColor(new Color(100, 200, 100, 200));
                g.fillRoundRect(20, 140, (int)(360 * (p / 100.0)), 20, 10, 10);
            });
            progress = (progress + 2) % 100;
            Thread.sleep(30); // ~33fps animation
        }
    }

    private static void clearConsole() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "chcp 65001 > nul & cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // Ignore if clear fails
        }
    }
}
