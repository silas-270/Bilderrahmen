package display;

/**
 * Game-Loop mit dynamischer Framerate-Steuerung.
 *
 * Zwei Modi (laut Spezifikation):
 *   IDLE  → 0,2 fps  (Bilderanzeige, keine Interaktion)
 *   MENU  → 30  fps  (Menü geöffnet, flüssige Darstellung)
 *
 * Der Loop läuft in einem eigenen Daemon-Thread.
 * Pro Frame wird {@link Renderer#render()} aufgerufen.
 */
public class RenderLoop {

    // -------------------------------------------------------------------------
    // Framerate-Konfiguration

    public enum Mode {
        /** Bilderanzeige – 0,2 fps → 5000 ms pro Frame. */
        IDLE(5_000L),
        /** Menü aktiv – 30 fps → ~33 ms pro Frame. */
        MENU(1_000L / 30),
        /** Bildübergang aktiv – 60 fps für smoothen Crossfade. */
        TRANSITION(1_000L / 60);

        final long frameTimeMs;

        Mode(long frameTimeMs) {
            this.frameTimeMs = frameTimeMs;
        }
    }

    // -------------------------------------------------------------------------

    private final Renderer renderer;

    private volatile Mode mode = Mode.IDLE;
    private volatile boolean running = false;

    private Thread loopThread;

    // -------------------------------------------------------------------------

    public RenderLoop(Renderer renderer) {
        this.renderer = renderer;
    }

    // -------------------------------------------------------------------------
    // Steuerung

    /** Startet den Render-Loop in einem Hintergrund-Thread. */
    public void start() {
        if (running) return;
        running = true;

        loopThread = new Thread(this::loop, "RenderLoop");
        loopThread.setDaemon(true);
        loopThread.start();
    }

    /** Stoppt den Render-Loop (wartet max. 2 Sekunden auf sauberes Beenden). */
    public void stop() {
        running = false;
        if (loopThread != null) {
            loopThread.interrupt();
            try {
                loopThread.join(2_000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Wechselt die Framerate-Stufe.
     * Kann jederzeit aus beliebigem Thread aufgerufen werden.
     */
    public void setMode(Mode mode) {
        this.mode = mode;
        // Den schlafenden Loop-Thread aufwecken, damit er sofort auf den
        // neuen Framerate-Wert reagiert (kein voller Sleep abgewartet).
        if (loopThread != null) {
            loopThread.interrupt();
        }
    }

    public Mode getMode() {
        return mode;
    }

    // -------------------------------------------------------------------------
    // Loop-Implementierung

    private void loop() {
        while (running) {
            long frameStart = System.currentTimeMillis();

            renderer.render();

            long elapsed = System.currentTimeMillis() - frameStart;
            long sleep   = mode.frameTimeMs - elapsed;

            if (sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    // setMode() hat den Thread geweckt – einfach weitermachen.
                    Thread.interrupted(); // interrupted-Flag löschen
                }
            }
        }
    }
}