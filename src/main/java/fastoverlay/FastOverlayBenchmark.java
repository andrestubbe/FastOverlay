package fastoverlay;

import javax.swing.JWindow;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class FastOverlayBenchmark {
    public static void main(String[] args) throws InterruptedException {
        clearConsole();
        System.out.println("FastOverlay v0.1.0");
        System.out.println("──────────────────────────────────────────────");
        System.out.println();
        System.out.println("Starting Benchmark: JWindow vs FastOverlay");
        
        // Screen Size ermitteln, um die Fenster mittig zu platzieren
        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        int screenW = screenSize.width;
        int screenH = screenSize.height;
        
        int winW = 300;
        int winH = 300;
        int yCenter = (screenH - winH) / 2;
        int xLeft = (screenW / 2) - winW - 50;
        int xRight = (screenW / 2) + 50;

        // Pre-render a single shared content image for both windows to guarantee identical pixels
        final BufferedImage sharedContent = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gC = sharedContent.createGraphics();
        gC.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gC.setColor(new Color(0, 255, 0, 150)); // GREEN!
        gC.fillOval(100, 100, 100, 100);
        gC.dispose();

        // Setup JWindow
        JWindow jWin = new JWindow();
        jWin.setBackground(new Color(0, 0, 0, 0));
        jWin.setSize(winW, winH);
        jWin.setLocation(xLeft, yCenter);
        jWin.setAlwaysOnTop(true);
        
        final float[] jx = {0};
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(sharedContent, 0, 0, null);
            }
        };
        panel.setOpaque(false);
        jWin.setContentPane(panel);
        jWin.setVisible(true);

        // Setup FastOverlay (Fullscreen for DComp Visual Movement test)
        System.out.println("Initializing FastOverlay...");
        FastOverlay.initEngine();
        FastOverlayWindow fWin = new FastOverlayWindow(0, 0, screenW, screenH, true, true);
        fWin.show();

        System.out.println("1. Messe JWindow für 3 Sekunden...");
        long startJ = System.currentTimeMillis();
        int framesJ = 0;
        long jTotalCpuNs = 0;
        long jTotalWallNs = 0;
        int baseX = (screenW / 2) - 150;
        
        com.sun.management.OperatingSystemMXBean osBean = 
            (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();

        // Render ONCE
        panel.paintImmediately(0, 0, 300, 300);

        while (System.currentTimeMillis() - startJ < 3000) {
            jx[0] = (jx[0] + 1) % 400;
            int currentX = baseX + (int)jx[0] - 200;
            
            long cpuBefore = osBean.getProcessCpuTime();
            long wallBefore = System.nanoTime();
            
            jWin.setLocation(currentX, yCenter);
            
            long wallAfter = System.nanoTime();
            long cpuAfter = osBean.getProcessCpuTime();
            
            jTotalWallNs += (wallAfter - wallBefore);
            jTotalCpuNs += (cpuAfter - cpuBefore);

            framesJ++;
            Thread.sleep(8); 
        }
        jWin.dispose();

        System.out.println("2. Messe FastOverlay für 3 Sekunden...");
        long totalCpuTimeF = 0;
        long fTotalWallNs = 0;
        
        // FastOverlay: Pre-upload the image ONCE outside the loop
        fWin.updateImage(sharedContent);

        long startF = System.currentTimeMillis();
        long[] jx2 = {0};
        int framesF = 0;
        while (System.currentTimeMillis() - startF < 3000) {
            jx2[0] = (jx2[0] + 1) % 400;
            int currentX = baseX + (int)jx2[0] - 200;

            long cpuBefore = osBean.getProcessCpuTime();
            long wallBefore = System.nanoTime();
            
            fWin.setVisualOffset(currentX, yCenter);
            
            long wallAfter = System.nanoTime();
            long cpuAfter = osBean.getProcessCpuTime();
            
            fTotalWallNs += (wallAfter - wallBefore);
            totalCpuTimeF += (cpuAfter - cpuBefore);

            framesF++;
            try { Thread.sleep(8); } catch (Exception e) {}
        }

        fWin.dispose();
        FastOverlay.disposeEngine();

        double jTimeMs = jTotalCpuNs / 1_000_000.0;
        double fTimeMs = totalCpuTimeF / 1_000_000.0;
        
        double jWallMs = jTotalWallNs / 1_000_000.0;
        double fWallMs = fTotalWallNs / 1_000_000.0;

        System.out.println("Benchmark finished!\n");
        System.out.printf("JWindow     : %,.3f ms (CPU) | %,.3f ms (Echte Zeit) | %.3f ms pro Frame\n", jTimeMs, jWallMs, (jWallMs / Math.max(1, framesJ)));
        System.out.printf("FastOverlay : %,.3f ms (CPU) | %,.3f ms (Echte Zeit) | %.3f ms pro Frame\n", fTimeMs, fWallMs, (fWallMs / Math.max(1, framesF)));

        System.out.println();
        if (fTimeMs < jTimeMs) {
            double factor = (fTimeMs > 0) ? (jTimeMs / fTimeMs) : Double.POSITIVE_INFINITY;
            if (Double.isInfinite(factor)) {
                System.out.println("=> FastOverlay gewinnt! Es benötigt praktisch 0,0 ms CPU-Zeit (Unendlichx schneller)!");
            } else {
                System.out.printf("=> FastOverlay gewinnt! Es ist %,.1fx schneller!\n", factor);
            }
        } else {
            double factor = jTimeMs / fTimeMs;
            System.out.printf("=> JWindow gewinnt! (%,.1fx schneller)\n", factor);
        }
        
        System.exit(0);
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
