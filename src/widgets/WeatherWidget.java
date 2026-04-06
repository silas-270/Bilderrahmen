package widgets;

import display.Renderer;

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
        if (data == null) return;
        
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
        // In einer echten App würde man dies per Timer machen,
        // für den Dummy-Call reicht es hier (bzw. wir rufen es einmal pro Frame auf)
        updateData();
        WeatherUtil.WeatherData data = WeatherUtil.fetchCurrentWeather();
        
        int w = 240;
        int h = 145;

        int margin = 25;
        int gap = 25;
        int x = margin;
        int y = screenH - (2 * h) - margin - gap;

        // Hintergrund, hellgrau halbtransparent
        g2.setColor(new Color(210, 210, 210, 160));
        g2.fillRoundRect(x, y, w, h, 25, 25);

        // --- Temperatur-Badge Dimensionen vorab berechnen ---
        String tempStr = data.temperature();
        Font tempFont = new Font("SansSerif", Font.BOLD, 22);
        g2.setFont(tempFont);
        FontMetrics fm = g2.getFontMetrics();

        int badgePadH = 18;
        int badgePadV = 1;
        int badgeW = fm.stringWidth(tempStr) + badgePadH * 2;
        int badgeH = fm.getAscent() + fm.getDescent() + badgePadV * 2;

        // Badge soll am unteren Ende des Icons sitzen → Icon-Größe
        // so berechnen, dass es fast die gesamte Widgethöhe ausfüllt,
        // aber Platz lässt, damit das Badge noch etwas in den Hintergrund ragt
        int iconSize = (int) (h * 1.1);

        // Horizontal zentriert, vertikal etwas über die Mitte
        int iconX = x + (w - iconSize) / 2;
        // "etwas über die Mitte": Icon endet ca. beim vertikalen Zentrum + badgeH/2
        // damit das Badge am unteren Rand des Icons positioniert werden kann
        int overlap = badgeH / 2; // Überlappung Icon-Badge
        int iconY = y + badgeH / 2 - (int) (overlap * 1.7);

        // Icon zeichnen
        if (weatherIcon != null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(weatherIcon, iconX, iconY, iconSize, iconSize, null);
        } else {
            g2.setColor(new Color(255, 215, 0));
            g2.fillRoundRect(iconX, iconY, iconSize, iconSize, 15, 15);
        }

        // --- Badge unten-mittig am Icon, überlappt leicht ---
        int badgeX = x + (w - badgeW) / 2;
        int badgeY = iconY + iconSize - overlap * 3; // überlappt den unteren Teil des Icons

        // Badge-Hintergrund: dunkel, semi-transparent, pill-förmig
        g2.setColor(new Color(100, 100, 100, 210));
        g2.fillRoundRect(badgeX, badgeY, badgeW, badgeH, badgeH, badgeH);

        // Badge-Text weiß
        g2.setFont(tempFont);
        g2.setColor(Color.WHITE);
        int textX = badgeX + badgePadH;
        int textY = badgeY + badgePadV + fm.getAscent();
        g2.drawString(tempStr, textX, textY);
    }
}
