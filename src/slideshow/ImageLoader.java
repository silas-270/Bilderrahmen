package slideshow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class ImageLoader {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    /**
     * Recursively loads all supported images from the given root path,
     * excluding anything inside a "Personen" subfolder.
     * Returns a shuffled ArrayList of ImageEntry.
     */
    public static ArrayList<ImageEntry> loadAll(Path root) {
        return load(root, true, root.resolve("Personen"));
    }

    /**
     * Loads only images from a specific person's subfolder.
     * e.g. root/Personen/Anna/
     */
    public static ArrayList<ImageEntry> loadPerson(Path root, String personName) {
        Path personFolder = root.resolve("Personen").resolve(personName);
        return load(personFolder, true, null);
    }

    /**
     * Returns the names of all subfolders inside root/Personen/.
     * Returns an empty list if the folder doesn't exist.
     */
    public static List<String> getPersonNames(Path root) {
        Path personenFolder = root.resolve("Personen");
        if (!Files.isDirectory(personenFolder)) return List.of();

        try (Stream<Path> entries = Files.list(personenFolder)) {
            return entries
                    .filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            System.err.println("Could not list Personen folder: " + e.getMessage());
            return List.of();
        }
    }

    // -------------------------------------------------------------------------

    private static ArrayList<ImageEntry> load(Path folder, boolean includeSubfolders, Path exclude) {
        ArrayList<ImageEntry> entries = new ArrayList<>();

        try (Stream<Path> stream = includeSubfolders
                ? Files.walk(folder)
                : Files.walk(folder).filter(p -> isDirectlyUnder(p, folder) || p.equals(folder))) {

            stream
                    .filter(Files::isRegularFile)
                    .filter(ImageLoader::isSupportedImage)
                    .filter(p -> exclude == null || !p.startsWith(exclude))
                    .map(ImageEntry::new)
                    .forEach(entries::add);

        } catch (IOException e) {
            System.err.println("Could not load images from " + folder + ": " + e.getMessage());
        }

        Collections.shuffle(entries);
        return entries;
    }

    private static boolean isSupportedImage(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        int dot = name.lastIndexOf('.');
        if (dot == -1) return false;
        return SUPPORTED_EXTENSIONS.contains(name.substring(dot + 1));
    }

    private static boolean isDirectlyUnder(Path path, Path parent) {
        return path.getParent() != null && path.getParent().equals(parent);
    }
}