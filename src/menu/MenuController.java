package menu;

import config.ConfigManager;
import display.LoadingOverlay;
import display.RenderLoop;
import display.Renderer;
import display.FullscreenWindow;
import slideshow.SlideshowManager;

import java.nio.file.Path;

/**
 * Zentrale Steuerungslogik für das Menü.
 *
 * Zuständigkeiten:
 *   - Menü öffnen / schließen (Toggle)
 *   - Framerate-Wechsel (IDLE ↔ MENU) beim Öffnen/Schließen
 *   - Personenliste beim Öffnen aktualisieren
 *   - Klick-Koordinaten auswerten und an die richtige Aktion weiterleiten
 *   - setPath(): Pfad-Wechsel mit automatischem Neuladen der Bilder
 */
public class MenuController {

    private final MenuOverlay overlay;
    private final Renderer renderer;
    private final RenderLoop renderLoop;
    private final SlideshowManager slideshow;
    private final ConfigManager config;
    private final LoadingOverlay loadingOverlay;
    private final FullscreenWindow window;

    /** Basis-Root-Pfad aus der Config (z.B. /home/user/Images). Wird bei Ordner-Wechsel aktualisiert. */
    private Path baseRoot;

    /**
     * Aktueller aktiver Pfad – wird durch setPath() verändert.
     * Entspricht baseRoot + relativem Pfad (z.B. /home/user/Images/Personen/Papa).
     */
    private Path activePath;

    // -------------------------------------------------------------------------

    public MenuController(MenuOverlay overlay,
                          Renderer renderer,
                          RenderLoop renderLoop,
                          SlideshowManager slideshow,
                          ConfigManager config,
                          LoadingOverlay loadingOverlay,
                          FullscreenWindow window,
                          Path root) {
        this.overlay        = overlay;
        this.renderer       = renderer;
        this.renderLoop     = renderLoop;
        this.slideshow      = slideshow;
        this.config         = config;
        this.loadingOverlay = loadingOverlay;
        this.window         = window;
        this.baseRoot       = root;
        this.activePath     = root;

        // Overlay als Menü-Painter im Renderer registrieren
        renderer.setMenuPainter(overlay);
    }

    // -------------------------------------------------------------------------
    // Pfad-Wechsel

    /**
     * Setzt den aktiven Pfad relativ zum Basis-Root und lädt die Bilder neu.
     * Schließt das Menü, zeigt den Lade-Spinner und lädt im Hintergrund.
     *
     * Beispiele:
     *   setPath("/")       → alle Bilder (ohne Personen-Ordner)
     *   setPath("/Papa")   → nur Bilder aus baseRoot/Personen/Papa
     *   setPath("/Mama")   → nur Bilder aus baseRoot/Personen/Mama
     *
     * @param relativePath Relativer Pfad: "/" für alles, "/Name" für eine Person
     */
    public void setPath(String relativePath) {
        // Menü sofort schließen und Spinner anzeigen
        close();
        loadingOverlay.show();
        renderLoop.setMode(RenderLoop.Mode.MENU); // 30 FPS für flüssigen Spinner

        // Im Hintergrund laden, damit der Spinner animiert bleibt
        Thread loader = new Thread(() -> {
            if (relativePath == null || relativePath.equals("/")) {
                activePath = baseRoot;
                slideshow.loadAll(baseRoot);
            } else {
                String personName = relativePath.startsWith("/")
                        ? relativePath.substring(1)
                        : relativePath;
                activePath = baseRoot.resolve("Personen").resolve(personName);
                slideshow.loadPerson(baseRoot, personName);
            }

            // Fertig → Spinner ausblenden, zurück auf IDLE
            loadingOverlay.hide();
            renderLoop.setMode(RenderLoop.Mode.IDLE);
        }, "PathLoader");
        loader.setDaemon(true);
        loader.start();
    }

    // -------------------------------------------------------------------------
    // Öffnen / Schließen

    /**
     * Schaltet das Menü um (Toggle).
     * Beim Öffnen: Personenliste aktualisieren, Framerate auf MENU.
     * Beim Schließen: Framerate zurück auf IDLE.
     */
    public void toggle() {
        if (renderer.isMenuVisible()) {
            close();
        } else {
            open();
        }
    }

    private void open() {
        overlay.updatePersonNames(baseRoot);
        renderer.setMenuVisible(true);
        renderLoop.setMode(RenderLoop.Mode.MENU);
    }

    private void close() {
        renderer.setMenuVisible(false);
        renderLoop.setMode(RenderLoop.Mode.IDLE);
    }

    // -------------------------------------------------------------------------
    // Klick-Auswertung

    /**
     * Verarbeitet einen Klick an den gegebenen Bildschirm-Koordinaten.
     * Prüft ob ein Menü-Button getroffen wurde und führt die Aktion aus.
     *
     * @param x Klick-X-Position
     * @param y Klick-Y-Position
     */
    public void handleClick(int x, int y) {
        if (!renderer.isMenuVisible()) {
            // Menü ist zu → Klick öffnet es
            toggle();
            return;
        }

        // Menü ist offen → prüfe ob ein Button getroffen wurde
        for (MenuOverlay.ButtonArea btn : overlay.getButtonAreas()) {
            if (btn.contains(x, y)) {
                executeAction(btn.action);
                return;
            }
        }

        // Klick außerhalb des Menüs → schließen
        close();
    }

    /**
     * Verarbeitet Mausbewegungen für Hover-Effekte.
     */
    public void handleMouseMove(int x, int y) {
        if (renderer.isMenuVisible()) {
            overlay.setHoverPosition(x, y);
        }
    }

    // -------------------------------------------------------------------------
    // Aktionen

    private void executeAction(String action) {
        switch (action) {
            case "select_all" -> {
                setPath("/");
            }
            case "choose_folder" -> {
                // Ordner-Auswahl-Dialog öffnen
                Path chosen = FolderChooser.chooseFolder(baseRoot);
                if (chosen != null) {
                    // Neuen Root persistent speichern
                    baseRoot = chosen;
                    config.setRootPath(chosen.toString());
                    config.save();

                    // Bilder aus neuem Root laden
                    setPath("/");
                } else {
                    // Abbruch → Menü einfach schließen
                    close();
                }
            }
            case "minimize" -> {
                close();
                window.minimize();
            }
            case "exit" -> {
                System.exit(0);
            }
            case "close" -> {
                close();
            }
            default -> {
                // Personen-Aktion: "person:Name"
                if (action.startsWith("person:")) {
                    String personName = action.substring("person:".length());
                    setPath("/" + personName);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Getter

    public Path getBaseRoot() {
        return baseRoot;
    }

    public Path getActivePath() {
        return activePath;
    }
}
