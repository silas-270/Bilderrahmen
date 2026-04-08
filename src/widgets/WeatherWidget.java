package widgets;

import display.Renderer;
import util.Scale;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import util.WeatherUtil;

public class WeatherWidget implements Renderer.OverlayPainter {

    private BufferedImage weatherIcon;
    private String lastIconName;

    public WeatherWidget() {
        updateData();
    }

    private void updateData() {
        WeatherUtil.WeatherData data = WeatherUtil.fetchCurrentWeather();
        if (data == null)
            return;

        if (!data.iconName().equals(lastIconName)) {
            try {
                weatherIcon = ImageIO.read(new File("resources/icons/weather/" + data.iconName() + ".png"));
                lastIconName = data.iconName();
            } catch (IOException e) {
                System.err.println("Wetter-Icon konnte nicht geladen werden: " + e.getMessage());
            }
        }
    }

    @Override
    public void paint(Graphics2D g2, int screenW, int screenH) {
        updateData();
        WeatherUtil.WeatherData data = WeatherUtil.fetchCurrentWeather();
        if (data == null)
            return;

        // --- MANUELLE X-POSITIONIERUNG (basierend auf 1080p, Y ist zentriert) ---
        int widgetW = 196;
        int widgetH = 118;
        int cornerR = 41; // Abrundung der Ecken

        int iconSize = 98;
        int iconX = 12; // X-Position des Icons innerhalb des Kastens

        int fontSize = 43;
        int textX = 115; // X-Position der Temperatur
        // ------------------------------------------------------------------------

        // Skalieren der Basis-Box
        int w = Scale.get(widgetW, screenH);
        int h = Scale.get(widgetH, screenH);
        int margin = Scale.get(22, screenH);
        int gap = Scale.get(22, screenH);

        int startX = margin;
        int startY = screenH - (2 * h) - margin - gap;

        // 1. Hintergrund zeichnen (Dunkelgrau)
        g2.setColor(new Color(40, 40, 40, 180));
        int arc = Scale.get(cornerR, screenH);
        g2.fillRoundRect(startX, startY, w, h, arc, arc);

        // 2. Icon zeichnen (V-Zentriert)
        int sIconSize = Scale.get(iconSize, screenH);
        int drawIconX = startX + Scale.get(iconX, screenH);
        int drawIconY = startY + (h - sIconSize) / 2;

        if (weatherIcon != null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(weatherIcon, drawIconX, drawIconY, sIconSize, sIconSize, null);
        } else {
            g2.setColor(new Color(255, 215, 0));
            g2.fillOval(drawIconX, drawIconY, sIconSize, sIconSize);
        }

        // 3. Temperatur zeichnen (V-Zentriert)
        Font tempFont = util.FontLoader.getFont(util.FontLoader.INTER_BOLD, Scale.font(fontSize, screenH));
        g2.setFont(tempFont);
        g2.setColor(Color.WHITE);

        FontMetrics fm = g2.getFontMetrics();
        int drawTextX = startX + Scale.get(textX, screenH);
        // Optische Zentrierung über die Ascent-Höhe
        int drawTextY = startY + (h + fm.getAscent()) / 2 - Scale.get(3, screenH);

        g2.drawString(data.temperature(), drawTextX, drawTextY);
    }
}
