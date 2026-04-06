package display;

import slideshow.ImageEntry;
import slideshow.SlideshowManager;
import util.ImageUtil;

import java.awt.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;

/**
 * Zentrales Zeichnen per Graphics2D + BufferStrategy (Active Rendering).
 *
 * Zuständigkeiten:
 *   - Hintergrundbild skaliert und rotiert (EXIF-Rotation via {@link ImageEntry}) zeichnen
 *   - Optionale Widget-Overlays zeichnen (Delegation an Widget-Interfaces)
 *   - Optionales Menü-Overlay zeichnen (Delegation an MenuOverlay-Interface)
 *
 * Um zirkuläre Abhängigkeiten zu vermeiden, werden MenuOverlay und
 * WidgetRenderer als funktionale Interfaces übergeben – der Renderer
 * weiß nichts über deren konkrete Implementierungen.
 */
public class Renderer {

    // -------------------------------------------------------------------------
    // Overlay-Callbacks (funktionale Interfaces für lose Kopplung)

    /** Alles, was zusätzlich über das Bild gezeichnet werden soll. */
    @FunctionalInterface
    public interface OverlayPainter {
        void paint(Graphics2D g2, int screenW, int screenH);
    }

    // -------------------------------------------------------------------------

    private final FullscreenWindow window;
    private final SlideshowManager slideshow;

    /** Wird gezeichnet, wenn das Menü sichtbar ist. Kann null sein. */
    private OverlayPainter menuPainter;

    /** Liste der Widget-Painter (Uhr, Wetter, Netzwerk …). */
    private final java.util.List<OverlayPainter> widgetPainters = new java.util.ArrayList<>();

    /** Steuert, ob das Menü aktuell angezeigt wird. */
    private volatile boolean menuVisible = false;

    /** Referenz auf den Loop (für Fallback von TRANSITION auf IDLE). */
    private RenderLoop renderLoop;

    // -------------------------------------------------------------------------

    public Renderer(FullscreenWindow window, SlideshowManager slideshow) {
        this.window    = window;
        this.slideshow = slideshow;
    }

    // -------------------------------------------------------------------------
    // Konfiguration

    public void setMenuPainter(OverlayPainter painter) {
        this.menuPainter = painter;
    }

    public void setRenderLoop(RenderLoop loop) {
        this.renderLoop = loop;
    }

    public void addWidgetPainter(OverlayPainter painter) {
        widgetPainters.add(painter);
    }

    public void setMenuVisible(boolean visible) {
        this.menuVisible = visible;
    }

    public boolean isMenuVisible() {
        return menuVisible;
    }

    // -------------------------------------------------------------------------
    // Rendering

    /**
     * Zeichnet einen vollständigen Frame.
     * Wird vom {@link RenderLoop} pro Tick aufgerufen.
     */
    public void render() {
        BufferStrategy bs = window.getBufferStrategy();
        if (bs == null) return;

        int screenW = window.getWidth();
        int screenH = window.getHeight();

        // Render-Loop: bei "lost" BufferStrategy erneut versuchen
        do {
            do {
                Graphics2D g2 = (Graphics2D) bs.getDrawGraphics();
                try {
                    drawFrame(g2, screenW, screenH);
                } finally {
                    g2.dispose();
                }
            } while (bs.contentsRestored());

            bs.show();
        } while (bs.contentsLost());
    }

    // -------------------------------------------------------------------------
    // Internes Zeichnen

    private void drawFrame(Graphics2D g2, int screenW, int screenH) {
        // Rendering-Qualität
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,          RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,      RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,       RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,  RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Schwarzer Hintergrund
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, screenW, screenH);

        // Status der Transition auslesen
        float progress = slideshow.getTransitionProgress();
        boolean inTransition = (progress < 1.0f);

        // Altes Bild (fadingImage) wird ausgeblendet
        BufferedImage fadingImg = slideshow.getFadingImage();
        ImageEntry fadingEntry = slideshow.getFadingEntry();

        if (fadingImg != null && fadingEntry != null) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f - progress));
            ImageUtil.drawFitAndRotated(g2, fadingImg, fadingEntry.getRotation(), screenW, screenH);
        }

        // Neues Bild zeichnen (einblenden, falls Transition aktiv)
        BufferedImage img   = slideshow.getCurrent();
        ImageEntry    entry = slideshow.getCurrentEntry();

        if (img != null && entry != null) {
            if (inTransition) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, progress));
            } else {
                g2.setComposite(AlphaComposite.SrcOver);
            }
            ImageUtil.drawFitAndRotated(g2, img, entry.getRotation(), screenW, screenH);
        }

        // Composite zurücksetzen für Widgets
        g2.setComposite(AlphaComposite.SrcOver);

        // Widgets (immer sichtbar)
        for (OverlayPainter wp : widgetPainters) {
            wp.paint(g2, screenW, screenH);
        }

        // Menü (nur wenn sichtbar)
        if (menuVisible && menuPainter != null) {
            menuPainter.paint(g2, screenW, screenH);
        }

        // Sobald die Transition fertig ist, den Loop wieder drosseln (falls er noch im Transition-Modus hängt)
        if (!inTransition && renderLoop != null && renderLoop.getMode() == RenderLoop.Mode.TRANSITION) {
            renderLoop.setMode(RenderLoop.Mode.IDLE);
        }
    }
}