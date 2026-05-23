package fastoverlay;

import fastoverlay.FastOverlay;

/**
 * Basic Hello World Demo for FastOverlay.
 */
public class Demo {
    public static void main(String[] args) {
        System.out.println("=== FastOverlay Demo ===");
        
        FastOverlay api = new FastOverlay();
        
        System.out.println("Calling native method...");
        api.doSomethingNative();
        
        System.out.println("=== Demo Complete ===");
    }
}
