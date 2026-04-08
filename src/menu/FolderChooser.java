package menu;

import javax.swing.JFileChooser;
import java.nio.file.Path;
import java.io.File;

/**
 * Nutzt javax.swing.JFileChooser, um eine Ordnerauswahl zu ermöglichen.
 * JFileChooser unterstützt im Gegensatz zu java.awt.FileDialog auf Linux/Windows
 * explizit die Auswahl von Verzeichnissen.
 */
public class FolderChooser {

    /**
     * Öffnet einen Dialog zur Ordnerauswahl.
     *
     * @param currentRoot Aktueller Pfad als Startpunkt
     * @return Der gewählte Pfad oder null bei Abbruch
     */
    public static Path chooseFolder(Path currentRoot) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Bilderordner auswählen");

        // WICHTIG: Dieser Modus erlaubt die Auswahl von Ordnern
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        // Startverzeichnis setzen
        if (currentRoot != null) {
            chooser.setCurrentDirectory(currentRoot.toFile());
        }

        // Dialog anzeigen (blockiert, bis der Nutzer fertig ist)
        int result = chooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            if (selectedFile != null) {
                return selectedFile.toPath();
            }
        }

        return null; // Nutzer hat abgebrochen oder Fenster geschlossen
    }
}