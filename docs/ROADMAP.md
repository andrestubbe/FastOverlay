# FastOverlay Roadmap 🗺️

**Vision:** High-performance, zero-latency DirectComposition overlay substrate for autonomous agents, robotics HUDs, and real-time UI debugging.

## 🟢 v0.1.0: Initial Release (Current)
- [x] **DirectComposition Engine**: Hardware-layered DWM window substrate with GPU compositing.
- [x] **Dynamic Click-Through**: Instant runtime toggling between click-through and solid input states.
- [x] **Visual Translation**: Sub-millisecond hardware offset manipulation without Win32 window movement.
- [x] **Swing Integration**: `FastOverlayPanel` child component for standard Java frames.
- [x] **FastCore Integration**: Automated DLL extraction and native loading.

## 🟡 v0.2.0: Primitives & Batching
- [ ] **Agent Vector Primitives**: Native C++ Direct2D primitives (`drawRect`, `drawLine`, `drawText`) via JNI batching.
- [ ] **DirectWrite Typography**: Sub-pixel anti-aliased font rendering directly on native surfaces.
- [ ] **Multi-Monitor Bounds**: Auto-detection and virtual desktop spanning across mixed DPI monitors.

## 🟠 v0.5.0: Platform Expansion
- [ ] **Linux Wayland / X11**: Wayland subsurface overlay and X11 composite window pass-through.
- [ ] **macOS Support**: `NSWindow` non-activating floating translucent panel substrate.

## 🔴 v1.0.0: Production Hardening
- [ ] **Full Stability & Memory Audit**: Long-run 120 FPS stress testing and leak verification.
- [ ] **Direct3D 11 Surface Sharing**: Zero-copy surface sharing with `FastScreen` and `FastVulkan`.

---

**Part of the FastJava Ecosystem**. *Making the JVM faster.* 🚀
