package config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Verantwortlich für das Lesen und Schreiben der JSON-Konfiguration
 * (Persistenz).
 */
public class ConfigManager {

    // Pfad zur persistenten Konfiguration
    private static final String CONFIG_FILE = "resources/config.json";

    // Gson-Instanz für die JSON-Verarbeitung (mit Pretty Printing für Lesbarkeit)
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // --- Konfigurierbare Parameter ---

    private String rootPath = "";
    private int imageDurationSeconds = 10;
    private int weatherRefreshIntervalSeconds = 1800; // 30 Minuten
    private int networkRefreshIntervalSeconds = 60; // 1 Minute
    private int transitionDurationMs = 800; // Überblendungsdauer in ms
    private double weatherLatitude = 48.1371;
    private double weatherLongitude = 11.5754;

    /**
     * Privater Konstruktor, damit die Instanz primär über load() erstellt wird.
     */
    private ConfigManager() {
    }

    /**
     * Lädt die persistente Konfiguration aus der JSON-Datei.
     * Wenn keine Datei existiert, wird eine neue mit Standardwerten erstellt.
     * * @return Eine Instanz des ConfigManagers
     */
    public static ConfigManager load() {
        if (!Files.exists(Paths.get(CONFIG_FILE))) {
            System.out.println("Keine config.json gefunden. Erstelle Standardkonfiguration...");
            ConfigManager defaultConfig = new ConfigManager();
            defaultConfig.save();
            return defaultConfig;
        }

        try (Reader reader = new FileReader(CONFIG_FILE)) {
            return gson.fromJson(reader, ConfigManager.class);
        } catch (IOException e) {
            System.err.println("Fehler beim Lesen der Konfigurationsdatei: " + e.getMessage());
            return new ConfigManager(); // Fallback auf Standardwerte
        }
    }

    /**
     * Speichert den aktuellen Zustand der Konfiguration persistent in der
     * JSON-Datei.
     */
    public void save() {
        try {
            // Stelle sicher, dass der resources-Ordner existiert
            Files.createDirectories(Paths.get("resources"));

            try (Writer writer = new FileWriter(CONFIG_FILE)) {
                gson.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("Fehler beim Schreiben der Konfigurationsdatei: " + e.getMessage());
        }
    }

    // --- Getter und Setter ---

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public int getImageDurationSeconds() {
        return imageDurationSeconds;
    }

    public void setImageDurationSeconds(int imageDurationSeconds) {
        this.imageDurationSeconds = imageDurationSeconds;
    }

    public int getWeatherRefreshIntervalSeconds() {
        return weatherRefreshIntervalSeconds;
    }

    public void setWeatherRefreshIntervalSeconds(int weatherRefreshIntervalSeconds) {
        this.weatherRefreshIntervalSeconds = weatherRefreshIntervalSeconds;
    }

    public int getNetworkRefreshIntervalSeconds() {
        return networkRefreshIntervalSeconds;
    }

    public void setNetworkRefreshIntervalSeconds(int networkRefreshIntervalSeconds) {
        this.networkRefreshIntervalSeconds = networkRefreshIntervalSeconds;
    }

    public int getTransitionDurationMs() {
        return transitionDurationMs;
    }

    public void setTransitionDurationMs(int transitionDurationMs) {
        this.transitionDurationMs = transitionDurationMs;
    }

    public double getWeatherLatitude() {
        return weatherLatitude;
    }

    public void setWeatherLatitude(double weatherLatitude) {
        this.weatherLatitude = weatherLatitude;
    }

    public double getWeatherLongitude() {
        return weatherLongitude;
    }

    public void setWeatherLongitude(double weatherLongitude) {
        this.weatherLongitude = weatherLongitude;
    }
}