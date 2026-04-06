package config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Model für das Mapping von MAC-Adressen zu Gerätenamen und IPs.
 * Liest die Zuordnungen als reinen Read-Only-Datenbestand aus der arp_mapping.json.
 * Anpassungen erfolgen ausschließlich manuell durch den Nutzer in der Datei.
 */
public class ArpMapping {

    public static class DeviceConfig {
        private String name;
        private String ip;

        public String getName() { return name; }
        public String getIp() { return ip; }
    }

    // Pfad zur Mapping-Datei
    private static final String MAPPING_FILE = "resources/arp_mapping.json";

    // Einfache Gson-Instanz reicht aus, da wir nicht schreiben (kein PrettyPrinting nötig)
    private static final Gson gson = new Gson();

    // Map speichert die Zuordnung: MAC-Adresse -> DeviceConfig (Read-Only)
    private final Map<String, DeviceConfig> deviceMap;

    /**
     * Privater Konstruktor.
     * Die übergebene Map wird als unveränderlich (unmodifiable) markiert,
     * um versehentliche Änderungen im Speicher zu verhindern.
     */
    private ArpMapping(Map<String, DeviceConfig> deviceMap) {
        this.deviceMap = deviceMap != null ? Collections.unmodifiableMap(deviceMap) : Collections.emptyMap();
    }

    /**
     * Lädt das ARP-Mapping (MAC/IP → Gerätename) aus der JSON-Datei.
     * @return Eine Instanz von ArpMapping mit den geladenen Daten.
     */
    public static ArpMapping load() {
        if (!Files.exists(Paths.get(MAPPING_FILE))) {
            System.out.println("Hinweis: arp_mapping.json nicht gefunden. Es werden nur rohe MAC-Adressen angezeigt.");
            return new ArpMapping(new HashMap<>());
        }

        try (Reader reader = new FileReader(MAPPING_FILE)) {
            Type type = new TypeToken<Map<String, DeviceConfig>>(){}.getType();
            Map<String, DeviceConfig> rawMap = gson.fromJson(reader, type);

            // Map normalisieren: MAC-Adressen zur Sicherheit in Uppercase umwandeln
            Map<String, DeviceConfig> normalizedMap = new HashMap<>();
            if (rawMap != null) {
                for (Map.Entry<String, DeviceConfig> entry : rawMap.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null && entry.getValue().getName() != null) {
                        normalizedMap.put(entry.getKey().toUpperCase().trim(), entry.getValue());
                    }
                }
            }

            return new ArpMapping(normalizedMap);
        } catch (IOException e) {
            System.err.println("Fehler beim Lesen der ARP-Mapping-Datei: " + e.getMessage());
            return new ArpMapping(new HashMap<>()); // Fallback auf leere Map
        }
    }

    /**
     * Gibt den hinterlegten Gerätenamen für eine bestimmte MAC-Adresse zurück.
     * @param macAddress Die MAC-Adresse des Geräts.
     * @return Der manuell eingetragene Gerätename oder ein Standardstring, falls unbekannt.
     */
    public String getDeviceName(String macAddress) {
        if (macAddress == null) return "Unbekannt";

        String normalizedMac = macAddress.toUpperCase().trim();
        DeviceConfig config = deviceMap.get(normalizedMac);
        return config != null ? config.getName() : "Unbekannt (" + normalizedMac + ")";
    }

    /**
     * Prüft, ob eine bestimmte MAC-Adresse in der manuellen Config hinterlegt ist.
     */
    public boolean isDeviceKnown(String macAddress) {
        if (macAddress == null) return false;
        return deviceMap.containsKey(macAddress.toUpperCase().trim());
    }

    /**
     * Gibt die gesamte (unveränderliche) Map der konfigurierten Geräte zurück.
     */
    public Map<String, DeviceConfig> getDeviceMap() {
        return deviceMap;
    }
}