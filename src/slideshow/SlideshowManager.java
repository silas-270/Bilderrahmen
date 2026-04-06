package slideshow;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import javax.imageio.ImageIO;

public class SlideshowManager {

    private ArrayList<ImageEntry> entries = new ArrayList<>();
    private int pointer = 0;

    private BufferedImage current;
    private BufferedImage preloaded;
    
    // --- Transition State ---
    private BufferedImage fadingImage;
    private ImageEntry fadingEntry;
    private long transitionStartTime = 0;
    private int transitionDurationMs = 0;

    // -------------------------------------------------------------------------
    // Loading

    /**
     * (Re)loads all images from the given root, excluding Personen folder.
     * Call this whenever the root path changes.
     */
    public void loadAll(Path root) {
        entries = ImageLoader.loadAll(root);
        pointer = 0;
        current = null;
        preloaded = null;
        resetTransition();
        if (!entries.isEmpty()) {
            loadCurrent();
            preloadNext();
        }
    }

    /**
     * Loads only images for a specific person.
     */
    public void loadPerson(Path root, String personName) {
        entries = ImageLoader.loadPerson(root, personName);
        pointer = 0;
        current = null;
        preloaded = null;
        resetTransition();
        if (!entries.isEmpty()) {
            loadCurrent();
            preloadNext();
        }
    }

    private void resetTransition() {
        fadingImage = null;
        fadingEntry = null;
        transitionStartTime = 0;
    }

    // -------------------------------------------------------------------------
    // Navigation

    /**
     * Advances to the next image. Wraps around at the end.
     * Starts a transition with the given duration.
     */
    public BufferedImage next(int durationMs) {
        if (entries.isEmpty()) return null;
        
        // Save current as fading image
        if (current != null) {
            fadingImage = current;
            fadingEntry = getCurrentEntry();
            transitionStartTime = System.currentTimeMillis();
            transitionDurationMs = durationMs;
        }
        
        int newPointer = (pointer + 1) % entries.size();

        // Neu mischen, wenn die Liste einmal komplett durchgelaufen ist
        if (newPointer == 0) {
            Collections.shuffle(entries);
        }

        pointer = newPointer;

        // Use preloaded image if it's ready, otherwise load synchronously
        if (preloaded != null) {
            current = preloaded;
            preloaded = null;
        } else {
            loadCurrent();
        }
        preloadNext();
        return current;
    }

    /**
     * Goes back to the previous image. Wraps around at the beginning.
     */
    public BufferedImage previous(int durationMs) {
        if (entries.isEmpty()) return null;
        
        if (current != null) {
            fadingImage = current;
            fadingEntry = getCurrentEntry();
            transitionStartTime = System.currentTimeMillis();
            transitionDurationMs = durationMs;
        }
        
        pointer = (pointer - 1 + entries.size()) % entries.size();
        preloaded = null; // preload is now stale
        loadCurrent();
        preloadNext();
        return current;
    }

    /**
     * Returns the current image without advancing.
     */
    public BufferedImage getCurrent() {
        return current;
    }

    /**
     * Returns the current ImageEntry (for rotation info etc.).
     */
    public ImageEntry getCurrentEntry() {
        if (entries.isEmpty()) return null;
        return entries.get(pointer);
    }
    
    public BufferedImage getFadingImage() { return fadingImage; }
    public ImageEntry getFadingEntry() { return fadingEntry; }
    
    public float getTransitionProgress() {
        if (transitionStartTime == 0 || transitionDurationMs <= 0) return 1.0f;
        float progress = (System.currentTimeMillis() - transitionStartTime) / (float) transitionDurationMs;
        if (progress >= 1.0f) {
            resetTransition();
            return 1.0f;
        }
        return progress;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    // -------------------------------------------------------------------------
    // Internal loading

    private void loadCurrent() {
        current = readImage(entries.get(pointer).path);
    }

    private void preloadNext() {
        if (entries.size() <= 1) return;
        int nextIndex = (pointer + 1) % entries.size();
        Path nextPath = entries.get(nextIndex).path;

        Thread preloadThread = new Thread(() -> {
            preloaded = readImage(nextPath);
        });
        preloadThread.setDaemon(true); // don't block JVM shutdown
        preloadThread.start();
    }

    private static BufferedImage readImage(Path path) {
        try {
            return ImageIO.read(path.toFile());
        } catch (IOException e) {
            System.err.println("Could not load image: " + path + " — " + e.getMessage());
            return null;
        }
    }
}