package widgets;

import display.Renderer;
import util.NetworkUtil;

import java.awt.*;
import java.util.List;

public class NetworkWidget implements Renderer.OverlayPainter {

    @Override
    public void paint(Graphics2D g2, int screenW, int screenH) {
        // Liste anwesender Personen über die Utility abrufen
        List<String> presentPersons = NetworkUtil.getPresentPersons();

        // Platzierungsparameter aus den anderen Widgets berechnen
        int boxH = 145;
        int margin = 25;
        int gap = 25;

        // Y-Koordinate des WeatherWidgets (zweite Box von unten)
        int weatherY = screenH - (2 * boxH) - margin - gap;

        // Das unterste Element startet direkt über dem WeatherWidget minus Abstand
        int bottomLimitY = weatherY - gap;

        // Horizontale Platzierung: Linksbündig zur Kante der Widgets,
        // leicht eingerückt (padding) für einen harmonischen Flow
        int x = margin;

        // Kein Hintergrundkasten, also nur Text formatieren
        Font font = new Font("SansSerif", Font.PLAIN, 28);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();

        int lineSpacing = 5; // Abstand zwischen den gestapelten Namen

        // Wir durchlaufen die Liste rückwärts, damit der letzte Name ganz unten steht
        for (int i = presentPersons.size() - 1; i >= 0; i--) {
            String name = presentPersons.get(i);

            // Die Baseline des Textes so berechnen, dass der Text maximal bis bottomLimitY
            // reicht
            int baselineY = bottomLimitY - fm.getDescent();

            // Leichter Text-Schatten (Drop-Shadow) zur besseren Lesbarkeit ohne
            // Hintergrund-Box!
            g2.setColor(new Color(0, 0, 0, 120));
            g2.drawString(name, x + 2, baselineY + 2);

            // Eigentlicher Text in Reinweiß
            g2.setColor(Color.WHITE);
            g2.drawString(name, x, baselineY);

            // Das Bottom-Limit rutscht für den nächsten (darüberliegenden) String nach oben
            bottomLimitY -= (fm.getAscent() + fm.getDescent() + lineSpacing);
        }
    }
}
