package display;

import java.awt.*;
import java.awt.geom.Arc2D;

/**
 * Animierter Lade-Spinner als Overlay.
 *
 * Zeigt einen rotierenden Bogen mit "Bilder werden geladen…" Text
 * auf schwarzem Hintergrund, solange die Slideshow lädt.
 *
 * Implementiert {@link Renderer.OverlayPainter} und kann als
 * Widget-Painter hinzugefügt/entfernt werden.
 */
public class LoadingOverlay implements Renderer.OverlayPainter {

    private static final Color BG           = Color.BLACK;
    private static final Color SPINNER_COLOR = new Color(100, 180, 255);
    private static final Color TEXT_COLOR    = new Color(180, 180, 180);

    private static final int SPINNER_SIZE = 48;
    private static final int ARC_LENGTH   = 90;      // Grad

    private volatile boolean visible = true;

    // -------------------------------------------------------------------------

    @Override
    public void paint(Graphics2D g2, int screenW, int screenH) {
        if (!visible) return;

        // Schwarzer Hintergrund (überdeckt alles)
        g2.setColor(BG);
        g2.fillRect(0, 0, screenW, screenH);

        // Animations-Winkel basierend auf Systemzeit (gleichmäßige Rotation)
        double rpm = 1.5; // Umdrehungen pro Sekunde
        long now = System.currentTimeMillis();
        int startAngle = (int) ((now * rpm * 360.0 / 1000.0) % 360);

        // Spinner zentriert zeichnen
        int cx = screenW / 2;
        int cy = screenH / 2 - 20;

        Graphics2D g2s = (Graphics2D) g2.create();
        g2s.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2s.setColor(SPINNER_COLOR);
        g2s.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        g2s.draw(new Arc2D.Double(
                cx - SPINNER_SIZE / 2.0, cy - SPINNER_SIZE / 2.0,
                SPINNER_SIZE, SPINNER_SIZE,
                startAngle, ARC_LENGTH,
                Arc2D.OPEN));

        g2s.dispose();

        // Text
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font font = new Font("SansSerif", Font.PLAIN, 16);
        g2.setFont(font);
        g2.setColor(TEXT_COLOR);
        String text = "Bilder werden geladen…";
        FontMetrics fm = g2.getFontMetrics();
        int textX = cx - fm.stringWidth(text) / 2;
        int textY = cy + SPINNER_SIZE / 2 + 30;
        g2.drawString(text, textX, textY);
    }

    // -------------------------------------------------------------------------

    /** Blendet den Spinner aus (Overlay bleibt registriert, zeichnet aber nichts). */
    public void hide() {
        this.visible = false;
    }

    /** Zeigt den Spinner wieder an (z.B. beim Ordner-Wechsel). */
    public void show() {
        this.visible = true;
    }

    public boolean isVisible() {
        return visible;
    }
}
