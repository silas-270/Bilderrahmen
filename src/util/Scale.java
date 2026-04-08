package util;

/**
 * Utility class for scaling absolute pixel values (based on 1920x1080)
 * to relative values based on the actual screen resolution.
 *
 * According to requirements:
 * - Reference resolution: 1920x1080
 * - Widgets/Overlays should scale ONLY based on height to preserve aspect ratio.
 */
public final class Scale {

    private static final double REF_H = 1080.0;

    private Scale() {}

    /**
     * Scales an absolute pixel value (defined for 1080p height) to the actual screen height.
     * This preserves the aspect ratio of components if used for both width and height.
     *
     * @param absoluteValue The value at 1080p resolution.
     * @param screenH The actual current screen height.
     * @return The scaled value.
     */
    public static int get(int absoluteValue, int screenH) {
        return (int) Math.round(absoluteValue * (screenH / REF_H));
    }

    /**
     * Scales a font size. Alias for {@link #get(int, int)}.
     */
    public static int font(int absoluteSize, int screenH) {
        return get(absoluteSize, screenH);
    }
    
    /**
     * Float version for higher precision if needed.
     */
    public static float getF(float absoluteValue, int screenH) {
        return (float) (absoluteValue * (screenH / REF_H));
    }
}
