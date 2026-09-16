package fastoverlay.benchmark;

import fastoverlay.FastOverlay;
import fastoverlay.FastOverlayWindow;
import org.openjdk.jmh.annotations.*;

import java.awt.Color;
import java.awt.Font;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class Benchmark {

    private FastOverlayWindow overlay;
    private int toggleState = 0;
    private int moveCoord = 100;

    @Setup(Level.Trial)
    public void setup() {
        FastOverlay.initEngine();
        // Create a 400x200 transparent, click-through, topmost overlay window
        overlay = new FastOverlayWindow(100, 100, 400, 200, true, true);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        FastOverlay.disposeEngine();
    }

    @org.openjdk.jmh.annotations.Benchmark
    public void benchmarkOverlayRepaint() {
        overlay.setPainter(g -> {
            g.setColor(new Color(16, 185, 129, 210));
            g.fillRoundRect(0, 0, 400, 200, 16, 16);
            g.setColor(Color.WHITE);
            g.drawString("JMH FastOverlay", 20, 50);
        });
    }

    @org.openjdk.jmh.annotations.Benchmark
    public void benchmarkPositionTranslation() {
        moveCoord = (moveCoord > 500) ? 100 : moveCoord + 1;
        overlay.setPosition(moveCoord, moveCoord);
    }

    @org.openjdk.jmh.annotations.Benchmark
    public void benchmarkToggleClickThrough() {
        toggleState ^= 1;
        overlay.setClickThrough(toggleState == 1);
    }
}
