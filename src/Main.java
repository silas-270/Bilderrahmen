import config.ConfigManager;
import display.FullscreenWindow;
import display.LoadingOverlay;
import display.RenderLoop;
import display.Renderer;
import menu.MenuController;
import menu.MenuOverlay;
import slideshow.SlideshowManager;
import widgets.ClockWidget;
import widgets.NetworkWidget;
import widgets.WeatherWidget;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;
import com.formdev.flatlaf.FlatDarkLaf;

/**
 * Einstiegspunkt für den digitalen Bilderrahmen.
 *
 * Verwendung:
 * java Main /pfad/zu/bildern (überschreibt gespeicherten Pfad einmalig)
 * java Main (nutzt persistenten Pfad aus config.json)
 */
public class Main {

    public static void main(String[] args) {
        // FlatLaf initialisieren für modernes Design
        FlatDarkLaf.setup();

        // Konfiguration laden (erstellt config.json mit Defaults falls nicht vorhanden)
        ConfigManager config = ConfigManager.load();

        // Pfad: CLI-Argument hat Vorrang, danach gespeicherter Pfad
        if (args.length > 0) {
            config.setRootPath(args[0]);
            config.save();
        }
        String rootStr = config.getRootPath();

        // Prüfen ob ein gültiger Pfad konfiguriert ist
        boolean hasValidRoot = rootStr != null
                && !rootStr.isBlank()
                && Files.isDirectory(Paths.get(rootStr));

        Path root = hasValidRoot ? Paths.get(rootStr) : Paths.get(".");

        // Slideshow (noch leer – wird im Hintergrund gefüllt)
        SlideshowManager slideshow = new SlideshowManager();

        // Display sofort starten
        FullscreenWindow window = new FullscreenWindow();
        Renderer renderer = new Renderer(window, slideshow);
        RenderLoop loop = new RenderLoop(renderer);
        renderer.setRenderLoop(loop);

        // Lade-Spinner einrichten
        LoadingOverlay loadingOverlay = new LoadingOverlay();
        renderer.addWidgetPainter(loadingOverlay);

        // Uhrzeit-Widget einrichten
        ClockWidget clockWidget = new ClockWidget();
        renderer.addWidgetPainter(clockWidget);

        // Wetter-Widget einrichten
        WeatherWidget weatherWidget = new WeatherWidget();
        renderer.addWidgetPainter(weatherWidget);

        // Netzwerk-Widget (Personenanzeige) einrichten
        NetworkWidget networkWidget = new NetworkWidget();
        renderer.addWidgetPainter(networkWidget);

        // Menü
        MenuOverlay menuOverlay = new MenuOverlay();
        MenuController menuController = new MenuController(
                menuOverlay, renderer, loop, slideshow, config, loadingOverlay, root);

        // Klick → Menü öffnen/schließen + Button-Auswertung
        window.setOnClickCallback(menuController::handleClick);

        // Mausbewegung → Hover-Effekte im Menü
        window.setOnMoveCallback(menuController::handleMouseMove);

        // Timer-Reset-Variable für automatischen Bildwechsel
        AtomicLong lastInteractionTime = new AtomicLong(System.currentTimeMillis());

        // Swipe-Gesten → Vor/Zurück (nur wenn Menü zu ist)
        window.setOnSwipeLeft(() -> {
            if (!renderer.isMenuVisible()) {
                slideshow.next(config.getTransitionDurationMs());
                loop.setMode(RenderLoop.Mode.TRANSITION); // Starte weiche Überblendung
                lastInteractionTime.set(System.currentTimeMillis());
            }
        });
        window.setOnSwipeRight(() -> {
            if (!renderer.isMenuVisible()) {
                slideshow.previous(config.getTransitionDurationMs());
                loop.setMode(RenderLoop.Mode.TRANSITION); // Starte weiche Überblendung
                lastInteractionTime.set(System.currentTimeMillis());
            }
        });

        if (!hasValidRoot) {
            // Kein gültiger Pfad → schwarzer Bildschirm + Menü öffnen
            loadingOverlay.hide();
            loop.setMode(RenderLoop.Mode.MENU);
            loop.start();
            menuController.toggle(); // Menü sofort öffnen
            return;
        }

        // Gültiger Pfad → Spinner anzeigen und Bilder im Hintergrund laden
        loop.setMode(RenderLoop.Mode.MENU);
        loop.start();

        Thread loader = new Thread(() -> {
            slideshow.loadAll(root);

            if (slideshow.isEmpty()) {
                // Ordner existiert, aber enthält keine Bilder → Menü öffnen
                System.err.println("Keine Bilder gefunden in: " + root);
                loadingOverlay.hide();
                menuController.toggle();
                return;
            }

            // Spinner ausblenden, zurück auf IDLE
            loadingOverlay.hide();
            loop.setMode(RenderLoop.Mode.IDLE);
            lastInteractionTime.set(System.currentTimeMillis()); // Timer starten nach Ladevorgang

            // Automatischer Bildwechsel starten
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(200); // Häufiger prüfen für reaktionsfreudigeren Timer
                    long now = System.currentTimeMillis();
                    long intervalMs = config.getImageDurationSeconds() * 1000L;

                    if (now - lastInteractionTime.get() >= intervalMs) {
                        lastInteractionTime.set(now);
                        slideshow.next(config.getTransitionDurationMs());
                        // Da der RenderLoop im IDLE ist (0,2fps), müssen wir ihn für den
                        // Übergang auf schnelle 60fps schalten.
                        if (!renderer.isMenuVisible()) {
                            loop.setMode(RenderLoop.Mode.TRANSITION);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "ImageLoader+SlideTimer");
        loader.setDaemon(true);
        loader.start();
    }
}