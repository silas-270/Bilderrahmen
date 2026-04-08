package menu;

import display.Renderer;
import slideshow.ImageLoader;
import util.Scale;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Zeichnet das Menü-Overlay per Graphics2D.
 *
 * Visueller Aufbau:
 * - Halbtransparenter dunkler Hintergrund (gesamter Bildschirm)
 * - Zentriertes Panel mit abgerundeten Ecken
 * - Menü-Einträge als Buttons:
 * „Alles" | [Personen…] | Beenden | ✕ Schließen
 *
 * Diese Klasse ist NUR für das Zeichnen zuständig.
 * Klick-Auswertung übernimmt {@link MenuController}.
 */
public class MenuOverlay implements Renderer.OverlayPainter {

    // -------------------------------------------------------------------------
    // Farben und Maße

    private static final Color BG_DIM = new Color(0, 0, 0, 160);
    private static final Color PANEL_BG = new Color(30, 30, 30, 230);
    private static final Color PANEL_BORDER = new Color(80, 80, 80, 180);
    private static final Color BUTTON_BG = new Color(55, 55, 55, 255);
    private static final Color BUTTON_HOVER = new Color(75, 75, 75, 255);
    private static final Color BUTTON_TEXT = new Color(230, 230, 230);
    private static final Color ACCENT = new Color(100, 180, 255);
    private static final Color DANGER_BG = new Color(180, 50, 50, 255);
    private static final Color DANGER_HOVER = new Color(220, 70, 70, 255);
    private static final Color HEADER_TEXT = new Color(255, 255, 255);
    private static final Color SEPARATOR = new Color(80, 80, 80, 120);

    private static final int PANEL_WIDTH = 400;
    private static final int PANEL_CORNER = 24;
    private static final int PANEL_PADDING = 30;
    private static final int BUTTON_HEIGHT = 50;
    private static final int BUTTON_CORNER = 25;
    private static final int BUTTON_GAP = 10;
    private static final int HEADER_HEIGHT = 60;
    private static final int SEPARATOR_HEIGHT = 20;

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
            this.label = label;
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

        // Skalierte Basiswerte berechnen (Widgets skalieren NUR nach Höhe)
        int panelW = Scale.get(PANEL_WIDTH, screenH);
        int panelPad = Scale.get(PANEL_PADDING, screenH);
        int btnH = Scale.get(BUTTON_HEIGHT, screenH);
        int btnGap = Scale.get(BUTTON_GAP, screenH);
        int headH = Scale.get(HEADER_HEIGHT, screenH);
        int sepH = Scale.get(SEPARATOR_HEIGHT, screenH);

        // 1. Hintergrund abdunkeln
        g2.setColor(BG_DIM);
        g2.fillRect(0, 0, screenW, screenH);

        // 2. Panel-Höhe berechnen
        int buttonCount = 4 + personNames.size();
        int contentHeight = headH
                + (buttonCount * btnH)
                + ((buttonCount - 1) * btnGap)
                + (sepH * 2);
        int panelHeight = panelPad * 2 + contentHeight;

        // 3. Panel zentrieren
        int panelX = (screenW - panelW) / 2;
        int panelY = (screenH - panelHeight) / 2;

        // 4. Panel zeichnen (Hintergrund + Rahmen)
        drawPanel(g2, panelX, panelY, panelW, panelHeight, screenH);

        // 5. Inhalt zeichnen
        int contentX = panelX + panelPad;
        int contentW = panelW - panelPad * 2;
        int y = panelY + panelPad;

        // Header
        y = drawHeader(g2, contentX, y, contentW, headH, screenH);

        // Button: „Alles"
        y = drawButton(g2, newAreas, contentX, y, contentW, btnH, "Alles", "select_all", ACCENT, false, screenH);
        y += btnGap;

        // Buttons: Personen
        for (String person : personNames) {
            y = drawButton(g2, newAreas, contentX, y, contentW, btnH, person, "person:" + person, BUTTON_TEXT, false,
                    screenH);
            y += btnGap;
        }

        // Separator
        y = drawSeparator(g2, contentX, y, contentW, sepH);

        // Button: „Ordner wählen…"
        y = drawButton(g2, newAreas, contentX, y, contentW, btnH, "Ordner wählen", "choose_folder", BUTTON_TEXT,
                false, screenH);
        y += btnGap;

        // Zweiter Separator vor Beenden
        y = drawSeparator(g2, contentX, y, contentW, sepH);

        // Buttons: Minimieren & Beenden (Nebeneinander)
        int halfW = (contentW - btnGap) / 2;
        drawButton(g2, newAreas, contentX, y, halfW, btnH, "Minimieren", "minimize", BUTTON_TEXT, false, screenH);
        drawButton(g2, newAreas, contentX + halfW + btnGap, y, halfW, btnH, "Beenden", "exit", HEADER_TEXT, true, screenH);
        y += btnH + btnGap;

        // Button: „✕ Schließen" (jetzt normal)
        drawButton(g2, newAreas, contentX, y, contentW, btnH, "Schließen", "close", BUTTON_TEXT, false, screenH);

        // Atomar veröffentlichen → Thread-safe für MenuController
        currentButtonAreas = List.copyOf(newAreas);
    }

    // -------------------------------------------------------------------------
    // Panel

    private void drawPanel(Graphics2D g2, int x, int y, int w, int h, int screenH) {
        int corner = Scale.get(PANEL_CORNER, screenH);
        RoundRectangle2D panelShape = new RoundRectangle2D.Float(x, y, w, h, corner, corner);

        // Schatten
        Graphics2D g2s = (Graphics2D) g2.create();
        int shadowOff = Scale.get(4, screenH);
        g2s.setColor(new Color(0, 0, 0, 80));
        g2s.fill(new RoundRectangle2D.Float(x + shadowOff, y + shadowOff, w, h, corner, corner));
        g2s.dispose();

        // Hintergrund
        g2.setColor(PANEL_BG);
        g2.fill(panelShape);

        // Rahmen
        g2.setColor(PANEL_BORDER);
        g2.setStroke(new BasicStroke(Scale.getF(1.5f, screenH)));
        g2.draw(panelShape);
    }

    // -------------------------------------------------------------------------
    // Header

    private int drawHeader(Graphics2D g2, int x, int y, int w, int headH, int screenH) {
        Font headerFont = util.FontLoader.getFont(util.FontLoader.OUTFIT_SEMIBOLD, Scale.font(22, screenH));
        g2.setFont(headerFont);
        g2.setColor(HEADER_TEXT);

        String title = "Menü";
        FontMetrics fm = g2.getFontMetrics();
        int textX = x + (w - fm.stringWidth(title)) / 2;
        int textY = y + fm.getAscent();
        g2.drawString(title, textX, textY);

        // Dekorative Linie unter dem Header
        int lineY = y + headH - Scale.get(10, screenH);
        g2.setColor(ACCENT);
        g2.setStroke(new BasicStroke(Scale.getF(2f, screenH)));
        int lineMargin = Scale.get(40, screenH);
        g2.drawLine(x + lineMargin, lineY, x + w - lineMargin, lineY);

        return y + headH;
    }

    // -------------------------------------------------------------------------
    // Button

    private int drawButton(Graphics2D g2, List<ButtonArea> areas, int x, int y, int w, int btnH,
            String label, String action, Color textColor, boolean isDanger, int screenH) {
        Rectangle bounds = new Rectangle(x, y, w, btnH);
        boolean hovered = bounds.contains(hoverX, hoverY);

        // Hintergrund
        Color bg;
        if (isDanger) {
            bg = hovered ? DANGER_HOVER : DANGER_BG;
        } else {
            bg = hovered ? BUTTON_HOVER : BUTTON_BG;
        }

        int corner = Scale.get(BUTTON_CORNER, screenH);
        RoundRectangle2D btnShape = new RoundRectangle2D.Float(
                x, y, w, btnH, corner, corner);

        g2.setColor(bg);
        g2.fill(btnShape);

        // Hover-Rahmen
        if (hovered) {
            g2.setColor(ACCENT);
            g2.setStroke(new BasicStroke(Scale.getF(1.5f, screenH)));
            g2.draw(btnShape);
        }

        // Text
        Font btnFont = util.FontLoader.getFont(util.FontLoader.INTER_REGULAR, Scale.font(16, screenH));
        g2.setFont(btnFont);
        g2.setColor(textColor);
        FontMetrics fm = g2.getFontMetrics();
        int textX = x + (w - fm.stringWidth(label)) / 2;
        int textY = y + (btnH - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(label, textX, textY);

        // Button-Bereich in die lokale Liste eintragen
        areas.add(new ButtonArea(bounds, action, label));

        return y + btnH;
    }

    // -------------------------------------------------------------------------
    // Separator

    private int drawSeparator(Graphics2D g2, int x, int y, int w, int sepH) {
        int lineY = y + sepH / 2;
        g2.setColor(SEPARATOR);
        g2.setStroke(new BasicStroke(1f));
        int margin = (int) (w * 0.025); // Kleiner relativer Margin
        g2.drawLine(x + margin, lineY, x + w - margin, lineY);
        return y + sepH;
    }
}