package util;

import java.awt.*;
import java.awt.geom.Path2D;

public class ChartUtil {

    /**
     * Zeichnet ein minimalistisches Diagramm.
     * 
     * @param g2 Die Graphics2D Instanz
     * @param x X-Koordinate der linken oberen Ecke
     * @param y Y-Koordinate der linken oberen Ecke
     * @param width Breite des Diagramms
     * @param height Höhe des Diagramms
     * @param data Array mit Double-Werten (z.B. Watt über Zeit)
     * @param color Die Primärfarbe für die Linie und Füllung
     */
    public static void drawMiniChart(Graphics2D g2, int x, int y, int width, int height, double[] data, Color color) {
        if (data == null || data.length < 2) return;

        // Maximalen Wert finden für Skalierung
        double max = 0;
        for (double v : data) {
            if (v > max) max = v;
        }
        if (max == 0) max = 1.0; // Verhindere Division durch Null

        // Basis-Linie (X-Achse)
        g2.setColor(new Color(255, 255, 255, 80));
        g2.setStroke(new BasicStroke(1));
        g2.drawLine(x, y + height, x + width, y + height);

        // Pfad für die Fläche und die Linie
        Path2D.Double areaPath = new Path2D.Double();
        Path2D.Double linePath = new Path2D.Double();

        double stepX = (double) width / (data.length - 1);
        
        for (int i = 0; i < data.length; i++) {
            double currX = x + i * stepX;
            double currY = y + height - (data[i] / max * height);

            if (i == 0) {
                linePath.moveTo(currX, currY);
                areaPath.moveTo(currX, y + height);
                areaPath.lineTo(currX, currY);
            } else {
                linePath.lineTo(currX, currY);
                areaPath.lineTo(currX, currY);
            }
            
            if (i == data.length - 1) {
                areaPath.lineTo(currX, y + height);
                areaPath.closePath();
            }
        }

        // 1. Fläche füllen (Halbtransparent)
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
        g2.fill(areaPath);

        // 2. Linie zeichnen (Solid)
        g2.setColor(color);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(2)); // Fest auf 2px für Minimalismus
        g2.draw(linePath);
    }
}
