package widgets;

import display.Renderer;

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

        // Etwas größere Proportionen
        int w = 240;
        int h = 145;
        
        // Unten links mit passendem Abstand
        int margin = 25;
        int x = margin;
        int y = screenH - h - margin;

        // Hellgrauer, halbtransparenter Hintergrund
        g2.setColor(new Color(210, 210, 210, 160));
        g2.fillRoundRect(x, y, w, h, 25, 25);

        // Angepasste Fonts
        Font timeFont = new Font("SansSerif", Font.BOLD, 75);
        Font dateFont = new Font("SansSerif", Font.PLAIN, 22);

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
        int spacing = 12;
        int totalHeight = timeAscent + spacing + dateAscent; 
        
        int startY = y + (h - totalHeight) / 2 + timeAscent - 5; 

        int timeX = x + (w - timeW) / 2;
        
        g2.setFont(timeFont);
        g2.drawString(timeStr, timeX, startY);

        // Datum zeichnen
        g2.setFont(dateFont);
        int dateW = dateFm.stringWidth(dateStr);
        int dateX = x + (w - dateW) / 2;
        int dateY = startY + spacing + (dateAscent / 2) + 5; 

        g2.drawString(dateStr, dateX, dateY);
    }
}
