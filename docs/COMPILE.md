# Building FastOverlay from Source

## Prerequisites

- **JDK 21+**: [Download](https://adoptium.net/)
- **Maven 3.9+**: [Download](https://maven.apache.org/download.cgi)
- **Visual Studio 2022 / 2026**: Community, Professional, Enterprise, or BuildTools with Desktop C++ workload

## Quick Build

```bash
# 1. Build native DLL first (Windows MSVC)
compile.bat

# 2. Build JAR with embedded DLL
mvn clean package -DskipTests
```

## Build Commands

| Command | Purpose |
|:---|:---|
| `compile.bat` | Build native `fastoverlay.dll` (Windows MSVC C++17) |
| `mvn clean compile` | Compile Java sources only |
| `mvn clean package` | Package JAR with embedded native DLL |
| `mvn test` | Run test suite |

## Native DLL Build

The `compile.bat` script:
- Automatically detects Visual Studio via `vswhere.exe`
- Resolves `JAVA_HOME` include headers
- Uses `native\fastoverlay.def` for explicit JNI symbol exports
- Outputs to `build\fastoverlay.dll`

Maven automatically bundles `build\fastoverlay.dll` inside the generated JAR. At runtime, `FastCore` unpacks the library into `%USERPROFILE%\.fastcore\native\fastoverlay\` and links it instantly.

---

**Part of the FastJava Ecosystem**. *Making the JVM faster.* 🚀
