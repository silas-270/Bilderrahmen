package util;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;

import java.nio.file.Path;

public class ExifUtil {

    /**
     * Determines the rotation of an image based on EXIF data.
     *
     * @param imagePath Path to the image file.
     * @return int: 0 (None), 1 (90° Left), 2 (180°), 3 (270°/90° Right)
     */
    public static int getRotation(Path imagePath) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(imagePath.toFile());
            ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                int orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
                return switch (orientation) {
                    case 1 -> 0; // Normal (0°)
                    case 8 -> 1; // Rotate 270 CW / 90° CCW (90° Left)
                    case 3 -> 2; // Rotate 180°
                    case 6 -> 3; // Rotate 90° CW (270° CCW / 90° Right)
                    default -> 0;
                };
            }
        } catch (Exception e) {
            System.err.println("Could not extract EXIF: " + e.getMessage());
        }
        return 0;
    }
}