package menu;

import display.Renderer;
import slideshow.ImageLoader;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Zeichnet das Menü-Overlay per Graphics2D.
 *
 * Visueller Aufbau:
 *   - Halbtransparenter dunkler Hintergrund (gesamter Bildschirm)
 *   - Zentriertes Panel mit abgerundeten Ecken
 *   - Menü-Einträge als Buttons:
 *       „Alles"  |  [Personen…]  |  Beenden  |  ✕ Schließen
 *
 * Diese Klasse ist NUR für das Zeichnen zuständig.
 * Klick-Auswertung übernimmt {@link MenuController}.
 */
public class MenuOverlay implements Renderer.OverlayPainter {

    // -------------------------------------------------------------------------
    // Farben und Maße

    private static final Color BG_DIM          = new Color(0, 0, 0, 160);
    private static final Color PANEL_BG        = new Color(30, 30, 30, 230);
    private static final Color PANEL_BORDER    = new Color(80, 80, 80, 180);
    private static final Color BUTTON_BG       = new Color(55, 55, 55, 255);
    private static final Color BUTTON_HOVER    = new Color(75, 75, 75, 255);
    private static final Color BUTTON_TEXT     = new Color(230, 230, 230);
    private static final Color ACCENT          = new Color(100, 180, 255);
    private static final Color CLOSE_BG        = new Color(180, 50, 50, 255);
    private static final Color CLOSE_HOVER     = new Color(220, 70, 70, 255);
    private static final Color HEADER_TEXT     = new Color(255, 255, 255);
    private static final Color SEPARATOR       = new Color(80, 80, 80, 120);

    private static final int PANEL_WIDTH        = 400;
    private static final int PANEL_CORNER       = 24;
    private static final int PANEL_PADDING      = 30;
    private static final int BUTTON_HEIGHT      = 50;
    private static final int BUTTON_CORNER      = 12;
    private static final int BUTTON_GAP         = 10;
    private static final int HEADER_HEIGHT      = 60;
    private static final int SEPARATOR_HEIGHT   = 20;

    // -------------------------------------------------------------------------
    // Zustand

    /** Aktuelle Button-Bereiche (wird pro Frame atomar ersetzt). Thread-safe. */
    private volatile List<ButtonArea> currentButtonAreas = List.of();

    /** Aktuelle Personen-Liste (wird beim Öffnen aktualisiert). */
    private List<String> personNames = List.of();

    /** Maus-Position für Hover-Effekte. -1 = keine Hover-Info. */
    private int hoverX = -1;
    private int hoverY = -1;

    // -------------------------------------------------------------------------
    // Datentyp für klickbare Bereiche

    /**
     * Beschreibt einen klickbaren Button im Menü.
     * Wird pro Frame neu berechnet und vom MenuController ausgelesen.
     */
    public static class ButtonArea {
        public final Rectangle bounds;
        public final String action;
        public final String label;

        public ButtonArea(Rectangle bounds, String action, String label) {
            this.bounds = bounds;
            this.action = action;
            this.label  = label;
        }

        public boolean contains(int x, int y) {
            return bounds.contains(x, y);
        }
    }

    // -------------------------------------------------------------------------
    // Konfiguration

    /**
     * Aktualisiert die Personenliste (aufgerufen vom MenuController beim Öffnen).
     */
    public void updatePersonNames(Path root) {
        this.personNames = ImageLoader.getPersonNames(root);
    }

    /**
     * Setzt die aktuelle Mausposition für Hover-Effekte.
     */
    public void setHoverPosition(int x, int y) {
        this.hoverX = x;
        this.hoverY = y;
    }

    /**
     * Gibt einen Thread-sicheren Snapshot der aktuellen Button-Bereiche zurück.
     * Wird vom MenuController genutzt, um Klicks auszuwerten.
     */
    public List<ButtonArea> getButtonAreas() {
        return currentButtonAreas;
    }

    // -------------------------------------------------------------------------
    // Zeichnen

    @Override
    public void paint(Graphics2D g2, int screenW, int screenH) {
        List<ButtonArea> newAreas = new ArrayList<>();

        // 1. Hintergrund abdunkeln
        g2.setColor(BG_DIM);
        g2.fillRect(0, 0, screenW, screenH);

        // 2. Panel-Höhe berechnen
        int buttonCount = 3 + personNames.size(); // „Alles" + Personen + „Ordner wählen" + „Beenden"
        int contentHeight = HEADER_HEIGHT
                + (buttonCount * BUTTON_HEIGHT)
                + ((buttonCount - 1) * BUTTON_GAP)
                + SEPARATOR_HEIGHT           // Separator vor „Ordner wählen"
                + BUTTON_HEIGHT              // Schließen-Button
                + BUTTON_GAP;
        int panelHeight = PANEL_PADDING * 2 + contentHeight;

        // 3. Panel zentrieren
        int panelX = (screenW - PANEL_WIDTH) / 2;
        int panelY = (screenH - panelHeight) / 2;

        // 4. Panel zeichnen (Hintergrund + Rahmen)
        drawPanel(g2, panelX, panelY, PANEL_WIDTH, panelHeight);

        // 5. Inhalt zeichnen
        int contentX = panelX + PANEL_PADDING;
        int contentW = PANEL_WIDTH - PANEL_PADDING * 2;
        int y = panelY + PANEL_PADDING;

        // Header
        y = drawHeader(g2, contentX, y, contentW);

        // Button: „Alles"
        y = drawButton(g2, newAreas, contentX, y, contentW, "Alles", "select_all", ACCENT, false);
        y += BUTTON_GAP;

        // Buttons: Personen
        for (String person : personNames) {
            y = drawButton(g2, newAreas, contentX, y, contentW, person, "person:" + person, BUTTON_TEXT, false);
            y += BUTTON_GAP;
        }

        // Separator
        y = drawSeparator(g2, contentX, y, contentW);

        // Button: „Ordner wählen…"
        y = drawButton(g2, newAreas, contentX, y, contentW, "📂  Ordner wählen…", "choose_folder", BUTTON_TEXT, false);
        y += BUTTON_GAP;

        // Button: „Beenden"
        y = drawButton(g2, newAreas, contentX, y, contentW, "Beenden", "exit", BUTTON_TEXT, false);
        y += BUTTON_GAP;

        // Button: „✕ Schließen"
        drawButton(g2, newAreas, contentX, y, contentW, "✕  Schließen", "close", HEADER_TEXT, true);

        // Atomar veröffentlichen → Thread-safe für MenuController
        currentButtonAreas = List.copyOf(newAreas);
    }

    // -------------------------------------------------------------------------
    // Panel

    private void drawPanel(Graphics2D g2, int x, int y, int w, int h) {
        RoundRectangle2D panelShape = new RoundRectangle2D.Float(x, y, w, h, PANEL_CORNER, PANEL_CORNER);

        // Schatten
        Graphics2D g2s = (Graphics2D) g2.create();
        g2s.setColor(new Color(0, 0, 0, 80));
        g2s.fill(new RoundRectangle2D.Float(x + 4, y + 4, w, h, PANEL_CORNER, PANEL_CORNER));
        g2s.dispose();

        // Hintergrund
        g2.setColor(PANEL_BG);
        g2.fill(panelShape);

        // Rahmen
        g2.setColor(PANEL_BORDER);
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(panelShape);
    }

    // -------------------------------------------------------------------------
    // Header

    private int drawHeader(Graphics2D g2, int x, int y, int w) {
        Font headerFont = new Font("SansSerif", Font.BOLD, 22);
        g2.setFont(headerFont);
        g2.setColor(HEADER_TEXT);

        String title = "Menü";
        FontMetrics fm = g2.getFontMetrics();
        int textX = x + (w - fm.stringWidth(title)) / 2;
        int textY = y + fm.getAscent();
        g2.drawString(title, textX, textY);

        // Dekorative Linie unter dem Header
        int lineY = y + HEADER_HEIGHT - 10;
        g2.setColor(ACCENT);
        g2.setStroke(new BasicStroke(2f));
        int lineMargin = 40;
        g2.drawLine(x + lineMargin, lineY, x + w - lineMargin, lineY);

        return y + HEADER_HEIGHT;
    }

    // -------------------------------------------------------------------------
    // Button

    private int drawButton(Graphics2D g2, List<ButtonArea> areas, int x, int y, int w,
                           String label, String action, Color textColor, boolean isClose) {
        Rectangle bounds = new Rectangle(x, y, w, BUTTON_HEIGHT);
        boolean hovered = bounds.contains(hoverX, hoverY);

        // Hintergrund
        Color bg;
        if (isClose) {
            bg = hovered ? CLOSE_HOVER : CLOSE_BG;
        } else {
            bg = hovered ? BUTTON_HOVER : BUTTON_BG;
        }

        RoundRectangle2D btnShape = new RoundRectangle2D.Float(
                x, y, w, BUTTON_HEIGHT, BUTTON_CORNER, BUTTON_CORNER);

        g2.setColor(bg);
        g2.fill(btnShape);

        // Hover-Rahmen
        if (hovered) {
            g2.setColor(ACCENT);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(btnShape);
        }

        // Text
        Font btnFont = new Font("SansSerif", Font.PLAIN, 16);
        g2.setFont(btnFont);
        g2.setColor(textColor);
        FontMetrics fm = g2.getFontMetrics();
        int textX = x + (w - fm.stringWidth(label)) / 2;
        int textY = y + (BUTTON_HEIGHT - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(label, textX, textY);

        // Button-Bereich in die lokale Liste eintragen
        areas.add(new ButtonArea(bounds, action, label));

        return y + BUTTON_HEIGHT;
    }

    // -------------------------------------------------------------------------
    // Separator

    private int drawSeparator(Graphics2D g2, int x, int y, int w) {
        int lineY = y + SEPARATOR_HEIGHT / 2;
        g2.setColor(SEPARATOR);
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(x + 10, lineY, x + w - 10, lineY);
        return y + SEPARATOR_HEIGHT;
    }
}
