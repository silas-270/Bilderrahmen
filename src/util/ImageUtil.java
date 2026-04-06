package util;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * Hilfsmethoden zum Skalieren und Drehen von BufferedImages.
 *
 * Rotationswerte (EXIF-Konvention aus {@link slideshow.ImageEntry}):
 *   0 → keine Rotation
 *   1 → 90° links  (CCW)
 *   2 → 180°
 *   3 → 90° rechts (CW)
 */
public final class ImageUtil {

    private ImageUtil() {}

    // -------------------------------------------------------------------------

    /**
     * Zeichnet {@code img} skaliert (uniformes Fit-Scaling / Letter-Pillarbox)
     * und EXIF-rotiert zentriert in den Zielbereich {@code (screenW × screenH)}.
     *
     * @param g2      Ziel-Graphics2D (Zustand wird nicht dauerhaft verändert)
     * @param img     Quellbild
     * @param rotation EXIF-Rotationswert (0–3, siehe Klassendoku)
     * @param screenW Breite des Zielbereichs in Pixeln
     * @param screenH Höhe des Zielbereichs in Pixeln
     */
    public static void drawFitAndRotated(Graphics2D g2,
                                         BufferedImage img,
                                         int rotation,
                                         int screenW,
                                         int screenH) {
        int imgW = img.getWidth();
        int imgH = img.getHeight();

        double angleDeg = rotationToDegrees(rotation);
        boolean swapAxes = (rotation == 1 || rotation == 3);

        // Effektive Abmessungen nach Rotation
        int effectiveW = swapAxes ? imgH : imgW;
        int effectiveH = swapAxes ? imgW : imgH;

        // Uniformes Fit-Scaling
        double scale = Math.min(
                (double) screenW / effectiveW,
                (double) screenH / effectiveH
        );

        int drawW = (int) (effectiveW * scale);
        int drawH = (int) (effectiveH * scale);

        // Zentrierung
        int offsetX = (screenW - drawW) / 2;
        int offsetY = (screenH - drawH) / 2;

        // Transformationsmatrix:
        //   1. Bildursprung in Bildmitte verschieben
        //   2. Rotieren
        //   3. Skalieren
        //   4. An Zielposition auf dem Bildschirm verschieben
        AffineTransform at = new AffineTransform();
        at.translate(offsetX + drawW / 2.0, offsetY + drawH / 2.0);
        at.rotate(Math.toRadians(angleDeg));
        at.scale(scale, scale);
        at.translate(-imgW / 2.0, -imgH / 2.0);

        Graphics2D g2copy = (Graphics2D) g2.create();
        try {
            g2copy.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2copy.drawImage(img, at, null);
        } finally {
            g2copy.dispose();
        }
    }

    // -------------------------------------------------------------------------

    /**
     * Wandelt den EXIF-Rotationswert (0–3) in Grad um.
     * 0→0°, 1→270° (=90° CCW), 2→180°, 3→90° (CW)
     */
    public static double rotationToDegrees(int rotation) {
        return switch (rotation) {
            case 1 -> 270.0; // 90° links
            case 2 -> 180.0;
            case 3 -> 90.0;  // 90° rechts
            default -> 0.0;
        };
    }
}