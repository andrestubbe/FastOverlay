package fastoverlay;

import javax.swing.*;
import java.awt.*;

public class FastOverlayPanelDemo {
    public static void main(String[] args) {
        System.out.println("Starting FastOverlayPanel Demo...");
        FastOverlay.initEngine();

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("FastOverlayPanel - Embedded GPU Canvas");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.setLayout(new BorderLayout());

            // Normal Swing UI on the left
            JPanel leftPanel = new JPanel();
            leftPanel.setBackground(Color.DARK_GRAY);
            leftPanel.setPreferredSize(new Dimension(200, 0));
            leftPanel.setLayout(new FlowLayout());
            
            JLabel title = new JLabel("Normal Swing UI");
            title.setForeground(Color.WHITE);
            leftPanel.add(title);
            
            JButton btn1 = new JButton("Click Me!");
            leftPanel.add(btn1);

            frame.add(leftPanel, BorderLayout.WEST);

            // FastOverlay GPU Panel in the center
            JPanel centerContainer = new JPanel();
            centerContainer.setLayout(new BorderLayout());
            centerContainer.setBackground(Color.BLACK);
            
            JLabel gpuTitle = new JLabel("FastOverlayPanel (100% GPU, DirectComposition, Zero-Copy)", SwingConstants.CENTER);
            gpuTitle.setForeground(Color.CYAN);
            gpuTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
            centerContainer.add(gpuTitle, BorderLayout.NORTH);

            FastOverlayPanel gpuPanel = new FastOverlayPanel();
            centerContainer.add(gpuPanel, BorderLayout.CENTER);

            frame.add(centerContainer, BorderLayout.CENTER);
            
            frame.setVisible(true);

            // Start Animation Thread for the GPU Panel
            new Thread(() -> {
                int ticks = 0;
                while (true) {
                    final int currentTick = ticks;
                    
                    // We can dynamically update the panel image
                    gpuPanel.setPainter(g -> {
                        int w = gpuPanel.getWidth();
                        int h = gpuPanel.getHeight();
                        
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        
                        // Clear background transparent
                        g.setColor(new Color(0, 0, 0, 0));
                        g.fillRect(0, 0, w, h);
                        
                        // Draw something cool
                        int size = 150 + (int)(Math.sin(currentTick * 0.05) * 50);
                        
                        g.setColor(new Color(100, 200, 255, 200));
                        g.fillRoundRect((w - size) / 2, (h - size) / 2, size, size, 30, 30);
                        
                        g.setColor(Color.WHITE);
                        g.setFont(new Font("Segoe UI", Font.BOLD, 24));
                        g.drawString("GPU Rendered", (w - 180)/2, h/2);
                    });
                    
                    ticks++;
                    try {
                        Thread.sleep(16); // 60 FPS
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        });
    }
}
