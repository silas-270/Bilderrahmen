package util;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Hilfsklasse zum zentralen Laden und Cachen von Custom Fonts.
 */
public class FontLoader {

    private static final Map<String, Font> fontCache = new HashMap<>();

    public static final String INTER_REGULAR = "resources/fonts/Inter-Regular.ttf";
    public static final String INTER_BOLD    = "resources/fonts/Inter-Bold.ttf";
    public static final String OUTFIT_SEMIBOLD = "resources/fonts/Outfit-SemiBold.ttf";

    /**
     * Lädt eine Font-Datei und gibt eine Instanz in der gewünschten Größe zurück.
     * Nutzt Caching, um mehrfaches Laden von der Festplatte zu vermeiden.
     */
    public static Font getFont(String path, float size) {
        String cacheKey = path;
        Font baseFont = fontCache.get(cacheKey);

        if (baseFont == null) {
            try {
                baseFont = Font.createFont(Font.TRUETYPE_FONT, new File(path));
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(baseFont);
                fontCache.put(cacheKey, baseFont);
            } catch (FontFormatException | IOException e) {
                System.err.println("[FontLoader] Fehler beim Laden der Font " + path + ": " + e.getMessage());
                return new Font("SansSerif", Font.PLAIN, (int) size);
            }
        }

        return baseFont.deriveFont(size);
    }
}
