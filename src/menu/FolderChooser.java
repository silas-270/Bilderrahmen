package menu;

import java.awt.FileDialog;
import java.awt.Frame;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Nutzt java.awt.FileDialog, um das echte, native Betriebssystem-Interface
 * zur Ordnerauswahl anzuzeigen (GTK auf Linux/Fedora, Win32 auf Windows).
 */
public class FolderChooser {

    /**
     * Öffnet einen nativen Dialog zur Ordnerauswahl.
     *
     * @param currentRoot Aktueller Pfad als Startpunkt
     * @return Der gewählte Pfad oder null bei Abbruch
     */
    public static Path chooseFolder(Path currentRoot) {
        // Dieser System-Property-Trick ist wichtig, damit AWT auf vielen
        // Plattformen (macOS/Linux) den "Ordner-Modus" aktiviert.
        System.setProperty("apple.awt.fileDialogForDirectories", "true");

        // Wir erstellen einen unsichtbaren Frame als Parent,
        // damit der Dialog ein ordentliches "Zuhause" hat.
        Frame parent = new Frame();
        FileDialog dialog = new FileDialog(parent, "Bilderordner auswählen", FileDialog.LOAD);

        // Startverzeichnis setzen
        if (currentRoot != null) {
            dialog.setDirectory(currentRoot.toAbsolutePath().toString());
        }

        // Dialog anzeigen (blockiert, bis der Nutzer fertig ist)
        dialog.setVisible(true);

        String directory = dialog.getDirectory();
        String file = dialog.getFile();

        // Ressourcen wieder freigeben
        parent.dispose();

        if (directory != null) {
            // Falls 'file' null ist (passiert bei manchen OS im Ordner-Modus),
            // nehmen wir nur das Directory.
            if (file == null) {
                return Paths.get(directory);
            }
            return Paths.get(directory, file);
        }

        return null; // Nutzer hat abgebrochen
    }
}