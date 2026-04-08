package widgets;

import display.Renderer;
import util.NetworkUtil;
import util.Scale;

import java.awt.*;
import java.util.List;

public class NetworkWidget implements Renderer.OverlayPainter {

    @Override
    public void paint(Graphics2D g2, int screenW, int screenH) {
        List<String> presentPersons = NetworkUtil.getPresentPersons();
        if (presentPersons.isEmpty()) return;

        // Skalierte Platzierungsparameter
        int boxH   = Scale.get(118, screenH);
        int margin = Scale.get(22, screenH);
        int gap    = Scale.get(22, screenH);
        int solarY = screenH - (3 * boxH) - margin - (2 * gap);

        // Das unterste Element startet über dem SolarWidget (Abstand soll 'gap' entsprechen)
        int currentY = solarY - gap;
        int x = margin;

        Font font = util.FontLoader.getFont(util.FontLoader.INTER_REGULAR, Scale.font(19, screenH));
        g2.setFont(font);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        FontMetrics fm = g2.getFontMetrics();

        int paddingX = Scale.get(18, screenH);
        int paddingY = Scale.get(11, screenH);
        int itemGap  = Scale.get(10, screenH);

        // Liste rückwärts durchlaufen (von unten nach oben)
        for (int i = presentPersons.size() - 1; i >= 0; i--) {
            String name = presentPersons.get(i);
            int textW = fm.stringWidth(name);
            int textH = fm.getAscent();

            int dotSize = Scale.get(8, screenH);
            int dotGap  = Scale.get(10, screenH);

            int pillW = textW + (2 * paddingX) + dotSize + dotGap;
            int pillH = textH + (2 * paddingY);
            int pillY = currentY - pillH;

            // 1. Pill Hintergrund
            g2.setColor(new Color(40, 40, 40, 160));
            g2.fillRoundRect(x, pillY, pillW, pillH, pillH, pillH);

            // 2. Status-Punkt (Sanftes Grün)
            g2.setColor(new Color(100, 255, 100, 200));
            int dotX = x + paddingX;
            int dotY = pillY + (pillH - dotSize) / 2;
            g2.fillOval(dotX, dotY, dotSize, dotSize);

            // 3. Text (Reinweiß, nach dem Punkt)
            g2.setColor(new Color(255, 255, 255, 220));
            g2.drawString(name, dotX + dotSize + dotGap, pillY + paddingY + textH - Scale.get(1, screenH));

            // Y-Position für das nächste Element nach oben verschieben
            currentY = pillY - itemGap;
        }
    }
}
