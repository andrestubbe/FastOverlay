# FastOverlay Reference

## 1. Engine & Subsystem Architecture
FastOverlay bypasses conventional AWT and Swing translucent window management (`setBackground(new Color(0,0,0,0))`), which suffers from severe GDI redraw stalls, flickering, and high EDT latency.

FastOverlay integrates directly with the native Windows Desktop Window Manager (DWM) through **DirectComposition** and hardware-layered windows (`WS_EX_LAYERED | WS_EX_TRANSPARENT | WS_EX_TOOLWINDOW`).

- **Hardware Layering**: Native Win32 layered surface rendered off-screen and composited by GPU.
- **DirectComposition Visual Offsets**: Smooth translation and animation without invoking heavyweight Win32 `SetWindowPos`.
- **Dynamic Input Transparency**: Seamlessly toggle between click-through (`WS_EX_TRANSPARENT`) and interactive click capturing at runtime.

---

## 2. API Specification

### FastOverlay (Static Lifecycle & Engine)
- `void initEngine()`: Initializes native DirectComposition/DWM subsystem resources.
- `void disposeEngine()`: Releases native engine pipelines and frees device contexts.
- `void setWindowProperties(long windowId, boolean alwaysOnTop, boolean clickThrough)`: Updates window z-order and transparency flags dynamically.
- `void updateWindowBitmap(long windowId, int[] pixels, int width, int height)`: Submits premultiplied 32-bit ARGB/BGRA pixel array to the native DWM surface.
- `void setVisualOffset(long windowId, int x, int y)`: Translates the DirectComposition visual tree node instantly via hardware.

### FastOverlayWindow (High-Level Window Handle)
- `FastOverlayWindow(int x, int y, int width, int height, boolean transparent, boolean topmost)`: Creates and positions a dedicated native hardware-accelerated overlay window.
- `void setClickThrough(boolean clickThrough)`: Toggles click transparency (true = clicks fall through to background apps, false = clicks intercepted).
- `void setTopmost(boolean topmost)`: Toggles topmost z-order layering.
- `void setPainter(Consumer<Graphics2D> painter)`: Renders custom Java2D vector drawings directly to the GPU surface.
- `void updateImage(BufferedImage img)`: Updates the overlay surface using a pre-allocated `BufferedImage`.
- `void setPosition(int x, int y)`: Moves the Win32 window bounds.
- `void setVisualOffset(int x, int y)`: Translates the visual node with sub-millisecond GPU speed.
- `void setSize(int width, int height)`: Resizes the overlay drawing canvas.
- `void show()`: Makes the overlay visible.
- `void hide()`: Hides the overlay from the screen.
- `void dispose()`: Destroys the native window and associated device contexts.

### FastOverlayPanel (Swing Integration)
- `FastOverlayPanel()`: Lightweight embedded Swing component backed by a native child window for hardware-composited canvas rendering inside standard frames.

---

## 3. Guarantees & Contracts
- **Zero-GC Hot Paths**: FastOverlay avoids per-frame object allocation during continuous rendering loops.
- **Premultiplied Alpha**: Pixel buffers are automatically converted to premultiplied format required by Win32 `UpdateLayeredWindowIndirect`.
- **Thread Safety**: Window lifecycle methods are safe for dispatch from background animation threads or the Swing EDT.

---

## 4. Platform Support

| Platform | Architecture | Status | Driver / Subsystem |
|:---|:---:|:---:|:---|
| **Windows 10 / 11** | x64 | ✅ Fully Supported | DirectComposition, Direct2D & Win32 Layered DWM |
| **Linux** | x64 / AArch64 | 🚧 Planned | Wayland Subsurfaces & X11 Composite Extension |
| **macOS** | Apple Silicon / x64 | 🚧 Planned | `NSWindow` Non-Activating Translucent Panels |

---

**Part of the FastJava Ecosystem**. *Making the JVM faster.* 🚀
