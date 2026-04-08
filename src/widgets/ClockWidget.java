package widgets;

import display.Renderer;
import util.Scale;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ClockWidget implements Renderer.OverlayPainter {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMAN);

    @Override
    public void paint(Graphics2D g2, int screenW, int screenH) {
        LocalDateTime now = LocalDateTime.now();
        String timeStr = now.format(TIME_FORMAT);
        String dateStr = now.format(DATE_FORMAT);

        // Skalierte Proportionen (196px Breite für perfekte Symmetrie im 4:3-Balken)
        int w = Scale.get(196, screenH);
        int h = Scale.get(118, screenH);

        // Unten links mit passendem Abstand
        int margin = Scale.get(22, screenH);
        int x = margin;
        int y = screenH - h - margin;

        // Dunkelgrauer, halbtransparenter Hintergrund
        g2.setColor(new Color(40, 40, 40, 180));
        int corner = Scale.get(41, screenH);
        g2.fillRoundRect(x, y, w, h, corner, corner);

        // Angepasste Fonts (Inter für Apple-Look)
        Font timeFont = util.FontLoader.getFont(util.FontLoader.INTER_BOLD, Scale.font(56, screenH));
        Font dateFont = util.FontLoader.getFont(util.FontLoader.INTER_REGULAR, Scale.font(18, screenH));

        g2.setColor(Color.WHITE);

        // Uhrzeit metrik
        g2.setFont(timeFont);
        FontMetrics timeFm = g2.getFontMetrics();
        int timeW = timeFm.stringWidth(timeStr);
        int timeAscent = timeFm.getAscent();

        // Datum metrik
        g2.setFont(dateFont);
        FontMetrics dateFm = g2.getFontMetrics();
        int dateAscent = dateFm.getAscent();

        // Zentrierung berechnen
        int spacing = Scale.get(12, screenH);
        int totalHeight = timeAscent + spacing + dateAscent;

        int startY = y + (h - totalHeight) / 2 + timeAscent - Scale.get(4, screenH);

        int timeX = x + (w - timeW) / 2;

        g2.setFont(timeFont);
        g2.drawString(timeStr, timeX, startY);

        // Datum zeichnen
        g2.setFont(dateFont);
        int dateW = dateFm.stringWidth(dateStr);
        int dateX = x + (w - dateW) / 2;
        int dateY = startY + spacing + (dateAscent / 2) + Scale.get(4, screenH);

        g2.drawString(dateStr, dateX, dateY);
    }
}
