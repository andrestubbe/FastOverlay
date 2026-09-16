# The Philosophy of FastOverlay

> [!IMPORTANT]
> **"Zero copies. Zero flicker. Hardware-accelerated DirectComposition. Native-first transparency."**

FastOverlay is built on the principle that modern Java applications require **native-first** acceleration for performance-critical UI overlays that standard JVM APIs do not support without severe latency and visual artifacting.

## Core Tenets

1. **Hardware-Accelerated DirectComposition**
   Bypass heavyweight Swing repaint managers and GDI blitting by rendering directly to native layered DWM surfaces with GPU-side visual transformations.

2. **True Click-Through Transparency**
   Provide genuine OS-level input pass-through (`WS_EX_TRANSPARENT`), allowing overlays to float invisibly above full-screen games, IDEs, and browser windows without stealing focus or disrupting mouse clicks.

3. **Deterministic Real-Time Frame Pacing**
   Achieve fluid 60 to 120 FPS animations without garbage collection stalls or EDT jitter.

4. **Blueprint Consistency**
   As part of the **FastJava** ecosystem, FastOverlay adheres to a standardized architecture:
   - **Native Backend**: Hand-tuned C++ with Direct2D/DirectComposition.
   - **Unified Loading**: Powered by `FastCore`.
   - **Zero Overhead**: Engineered for autonomous AI agent feedback, vision robotics, and debugging HUDs.

---

**Part of the FastJava Ecosystem**. *Making the JVM faster.* 🚀
