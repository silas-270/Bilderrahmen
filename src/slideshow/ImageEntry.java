package slideshow;

import util.ExifUtil;

import java.nio.file.Path;

public class ImageEntry {

    final Path path;
    final int rotation; // 0 = None, 1 = 90° Left, 2 = 180°, 3 = 90° Right

    ImageEntry(Path path) {
        this.path = path;
        this.rotation = ExifUtil.getRotation(path);
    }

    public int getRotation() {
        return rotation;
    }
}