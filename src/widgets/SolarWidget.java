package widgets;

import display.Renderer;
import util.Scale;
import java.awt.*;

public class SolarWidget implements Renderer.OverlayPainter {

    private boolean showDaily = false;

    /**
     * Verarbeitet einen Klick. Wenn der Klick innerhalb des Widgets liegt,
     * wird der Modus umgeschaltet und true zurückgegeben.
     */
    public boolean handleClick(int mouseX, int mouseY, int screenW, int screenH) {
        int w = Scale.get(196, screenH);
        int h = Scale.get(118, screenH);
        int margin = Scale.get(22, screenH);
        int gap = Scale.get(22, screenH);

        int widgetX = margin;
        int widgetY = screenH - (3 * h) - margin - (2 * gap);

        if (mouseX >= widgetX && mouseX <= widgetX + w &&
                mouseY >= widgetY && mouseY <= widgetY + h) {
            showDaily = !showDaily;
            return true;
        }
        return false;
    }

    @Override
    public void paint(Graphics2D g2, int screenW, int screenH) {
        // Skalierte Proportionen
        int w = Scale.get(196, screenH);
        int h = Scale.get(118, screenH);
        int margin = Scale.get(22, screenH);
        int gap = Scale.get(22, screenH);

        int x = margin;
        int y = screenH - (3 * h) - margin - (2 * gap);

        // 1. Hintergrund zeichnen (Halbtransparentes Anthrazit)
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(40, 40, 40, 180));
        int arc = Scale.get(41, screenH);
        g2.fillRoundRect(x, y, w, h, arc, arc);

        // Daten laden
        util.SolarUtil.SolarData data = util.SolarUtil.fetchSolarData();
        Color accentColor = new Color(255, 215, 0); // Solar Gelb

        // 2. Modus-Label (oben links)
        g2.setFont(util.FontLoader.getFont(util.FontLoader.INTER_BOLD, Scale.font(11, screenH)));
        g2.setColor(new Color(255, 255, 255, 120));
        g2.drawString(showDaily ? "ERTRAG HEUTE" : "AKTUELL", x + Scale.get(22, screenH), y + Scale.get(28, screenH));

        // 3. Haupt-Wert (Mittig platziert)
        String value;
        String unit;
        if (showDaily) {
            value = String.format("%.1f", data.getDailyEnergy());
            unit = "kWh";
        } else {
            double p = data.getCurrentPower();
            if (p >= 1000) {
                value = String.format("%.1f", p / 1000.0);
                unit = "kW";
            } else {
                value = String.valueOf((int) p);
                unit = "W";
            }
        }

        // Fonts
        Font valFont = util.FontLoader.getFont(util.FontLoader.INTER_BOLD, Scale.font(42, screenH));
        Font unitFont = util.FontLoader.getFont(util.FontLoader.INTER_REGULAR, Scale.font(18, screenH));

        g2.setFont(valFont);
        FontMetrics fmVal = g2.getFontMetrics();
        g2.setFont(unitFont);

        int startX = x + Scale.get(22, screenH); // Linksbündig
        int baseLineY = y + Scale.get(70, screenH); // Etwas höher für mehr Abstand zum Chart

        // Wert zeichnen
        g2.setColor(Color.WHITE);
        g2.setFont(valFont);
        g2.drawString(value, startX, baseLineY);

        // Einheit zeichnen (etwas versetzt und dezenter)
        g2.setFont(unitFont);
        g2.setColor(new Color(255, 255, 255, 180));
        g2.drawString(unit, startX + fmVal.stringWidth(value) + Scale.get(6, screenH), baseLineY);

        // 4. Diagramm (ganz unten, mit mehr Abstand)
        int chartW = w - Scale.get(44, screenH);
        int chartH = Scale.get(20, screenH); // Etwas flacher
        int chartX = x + Scale.get(22, screenH);
        int chartY = y + h - chartH - Scale.get(14, screenH); // Tiefer gesetzt

        util.ChartUtil.drawMiniChart(g2, chartX, chartY, chartW, chartH, data.getHistory(), accentColor);
    }
}
