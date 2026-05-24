# FastOverlay v0.1.0 [ALPHA] — High-Performance Native Overlay API for Java

[![Status](https://img.shields.io/badge/status-v0.1.0-brightgreen.svg)](https://github.com/andrestubbe/FastOverlay/releases/tag/v0.1.0)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010+-lightgrey.svg)]()
[![JitPack](https://img.shields.io/badge/JitPack-ready-green.svg)](https://jitpack.io/#andrestubbe)

**⚡ A zero-latency native overlay module for the FastJava ecosystem. Create click-through, hardware-accelerated windows
for visual feedback.**

FastOverlay provides an ultra-fast way to draw on top of other applications. Built for bot visualization, UI
debugging, and AI-driven feedback loops that require flicker-free native transparency.

[![FastKeyboard Showcase](docs/screenshot.png)](https://www.youtube.com/watch?v=BZsqQl7WqWk)


---

## Table of Contents

- [Features](#features)
- [Installation](#installation)
- [License](#license)

## Features

- **✨ Native Transparency**: Hardware-accelerated, click-through overlay windows.
- **⚡ Zero Latency**: Direct DirectX/GDI+ rendering bypassing Swing/AWT.
- **📦 Focus Agnostic**: Completely non-intrusive native window management.
- **🚀 Smooth Rendering**: Flicker-free visualization for real-time path drawing.

## Installation

### Option 1: Maven (Recommended)

Add the JitPack repository and the dependencies to your `pom.xml`:

```xml

<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
<!-- FastOverlay Library -->
<dependency>
    <groupId>com.github.andrestubbe</groupId>
    <artifactId>fastoverlay</artifactId>
    <version>v0.1.0</version>
</dependency>

<!-- FastCore (Required Native Loader) -->
<dependency>
    <groupId>com.github.andrestubbe</groupId>
    <artifactId>fastcore</artifactId>
    <version>v0.1.0</version>
</dependency>
</dependencies>
```

### Option 2: Gradle (via JitPack)

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.andrestubbe:fastoverlay:v0.1.0'
    implementation 'com.github.andrestubbe:fastcore:v0.1.0'
}
```

### Option 3: Direct Download (No Build Tool)

Download the latest JARs directly to add them to your classpath:

1. 📦 *
   *[fastoverlay-v0.1.0.jar](https://github.com/andrestubbe/FastOverlay/releases/download/v0.1.0/fastoverlay-v0.1.0.jar)
   ** (The Core Library)
2. ⚙️ **[fastcore-v0.1.0.jar](https://github.com/andrestubbe/FastCore/releases/download/v0.1.0/fastcore-v0.1.0.jar)** (
   The Mandatory Native Loader)

> [!IMPORTANT]
> All JARs must be in your classpath for the native JNI calls to function correctly.

---

## License

MIT License — See [LICENSE](LICENSE) file for details.

---

## Related Projects

- [FastCore](https://github.com/andrestubbe/FastCore) — Native Library Loader for Java
- [FastKeyboard](https://github.com/andrestubbe/FastKeyboard) — High-performance RawInput engine
- [FastTheme](https://github.com/andrestubbe/FastTheme) — Advanced UI styling engine

---
**Part of the FastJava Ecosystem** — *Making the JVM faster.*


