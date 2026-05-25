# FastOverlay

## 1. Vision & Kernidee
**FastOverlay** ist das native Modul, um Hardware-beschleunigte, transparente Zeichnungen über den gesamten Windows-Desktop zu legen, ohne die darunterliegenden Fenster zu blockieren.

Während **FastGhostMouse** speziell für die extrem latenzfreie Darstellung eines sekundären Cursors (120Hz+) optimiert wurde, ist FastOverlay der generische "Zeichenblock" für den KI-Agenten. 
Der Agent kann damit visuelles Feedback geben: Wenn er ein UI-Element gefunden hat, zeichnet er eine rote Bounding-Box darum, bevor er klickt. Wenn er einen Fehler erkennt, schreibt er roten Text direkt auf den Bildschirm.

**Warum DirectComposition / DWM?**
Da du DirectComposition bereits bei `FastGhostMouse` erfolgreich getestet hast, weißt du: Es ist der einzige Weg unter Windows, um echtes VSync-sauberes, Tearing-freies und hochperformantes Alpha-Blending ohne Flickern hinzubekommen. Ein klassisches Java-`JFrame` mit `setBackground(new Color(0,0,0,0))` ist unter Windows notorisch langsam und fehleranfällig.

## 2. Java High-Level API

```java
public interface FastOverlay {
    static FastOverlay open() { return new FastOverlayImpl(); }

    // Löscht alle aktuellen Zeichnungen
    void clear();

    // Primitives für den Agenten
    void drawRect(int x, int y, int width, int height, int color, int thickness);
    void drawText(int x, int y, String text, int fontSize, int color);
    void drawLine(int x1, int y1, int x2, int y2, int color, int thickness);

    // Pusht die Änderungen auf den Bildschirm (Commit / Present)
    void present();
}
```

## 3. C++ JNI Backend
Das Backend baut auf deinen Erfahrungen mit `FastGhostMouse` auf:

1. **Layered Window:** Ein unsichtbares, transparentes Fenster (`WS_EX_LAYERED | WS_EX_TRANSPARENT | WS_EX_TOOLWINDOW`) über den gesamten virtuellen Desktop. `WS_EX_TRANSPARENT` sorgt dafür, dass alle Maus-Klicks einfach hindurchfallen.
2. **Direct2D & DirectComposition:** Ein `ID2D1DeviceContext` wird genutzt, um Hardware-beschleunigt Primitiven (Rechtecke, Linien, Text via DirectWrite) in einen Surface-Buffer zu zeichnen. `IDCompositionDevice::Commit()` schiebt die Änderungen sofort zum Desktop Window Manager (DWM).
3. **Zero-GC Batching:** Um JNI-Overhead zu vermeiden, können Zeichnungen als Batch (Array aus Structs) an C++ übergeben werden.

## 4. Agent-Kit (KI-Integration)
Das Overlay ist das "Debug-UI" des Agenten. 

**JSON Schema Beispiel (Highlighting):**
```json
{
  "action": "overlay_highlight",
  "rectangles": [
    { "x": 100, "y": 200, "w": 50, "h": 20, "color": "0xFFFF0000", "thickness": 2 }
  ],
  "duration_ms": 1500
}
```
