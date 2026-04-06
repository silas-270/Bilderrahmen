package util;

import config.ArpMapping;
import config.ConfigManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Hilfsklasse für Netzwerk-Operationen.
 *
 * Strategie:
 *   1. Das eigene Subnetz (z.B. /24) ermitteln.
 *   2. Alle 254 Hosts parallel anpingen → ARP-Cache des Kernels wird gefüllt.
 *   3. /proc/net/arp lesen und MACs mit dem ArpMapping abgleichen.
 *   4. Gefundene Namen in den Cache schreiben.
 *
 * Der Refresh läuft alle REFRESH_INTERVAL_MS im Hintergrund, damit der Paint-Thread
 * nie blockiert.
 */
public class NetworkUtil {

    private static final int  PING_TIMEOUT_S      = 1;
    private static final String ARP_TABLE_PATH    = "/proc/net/arp";

    private static final CopyOnWriteArrayList<String> cachedPersons = new CopyOnWriteArrayList<>();
    private static volatile boolean started = false;

    // -------------------------------------------------------------------------

    public static List<String> getPresentPersons() {
        ensureStarted();
        return List.copyOf(cachedPersons);
    }

    // -------------------------------------------------------------------------

    private static synchronized void ensureStarted() {
        if (started) return;
        started = true;

        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    refresh();
                    int intervalSeconds = ConfigManager.load().getNetworkRefreshIntervalSeconds();
                    // Fallback auf 60 Sekunden, falls unvernünftig kleiner Wert in Config steht
                    if (intervalSeconds < 5) intervalSeconds = 60;
                    Thread.sleep(intervalSeconds * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "NetworkUtil-Refresh");

        thread.setDaemon(true);
        thread.start();
    }

    // -------------------------------------------------------------------------

    private static void refresh() {
        ArpMapping mapping = ArpMapping.load();
        Map<String, ArpMapping.DeviceConfig> deviceMap = mapping.getDeviceMap();
        if (deviceMap.isEmpty()) return;

        // Alle konfigurierten IPs direkt und parallel anpingen
        ExecutorService pool = Executors.newCachedThreadPool();
        for (ArpMapping.DeviceConfig config : deviceMap.values()) {
            if (config != null && config.getIp() != null && !config.getIp().isBlank()) {
                final String ip = config.getIp();
                pool.submit(() -> pingIp(ip));
            }
        }
        pool.shutdown();
        try {
            // Warten bis alle Pings (max PING_TIMEOUT_S) durch sind
            pool.awaitTermination(PING_TIMEOUT_S + 1L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        // ARP-Tabelle lesen (jetzt mit frischen Einträgen der angepingten IPs)
        Map<String, String> arpTable = readArpTable(); // MAC (uppercase) → IP

        // Abgleich mit dem konfigurierten Mapping (über MAC-Adresse aus dem ARP-Cache)
        List<String> found = new ArrayList<>();
        for (Map.Entry<String, String> arpEntry : arpTable.entrySet()) {
            String mac = arpEntry.getKey(); 
            if (deviceMap.containsKey(mac)) {
                found.add(deviceMap.get(mac).getName());
            }
        }

        cachedPersons.clear();
        cachedPersons.addAll(found);
        System.out.println("[NetworkUtil] Anwesend: " + found);
    }

    // -------------------------------------------------------------------------

    /**
     * Liest /proc/net/arp.
     * @return Map von MAC-Adresse (uppercase) → IP-Adresse.
     *         Enthält nur vollständige Einträge (Flags != 0x0).
     */
    private static Map<String, String> readArpTable() {
        if (!Files.exists(Paths.get(ARP_TABLE_PATH))) {
            System.err.println("[NetworkUtil] /proc/net/arp nicht gefunden.");
            return Collections.emptyMap();
        }

        Map<String, String> result = new java.util.HashMap<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(ARP_TABLE_PATH));
            // Zeile 0 ist Header: "IP address  HW type  Flags  HW address  Mask  Device"
            for (int i = 1; i < lines.size(); i++) {
                String[] cols = lines.get(i).trim().split("\\s+");
                if (cols.length < 6) continue;

                String ip    = cols[0];
                String flags = cols[2];
                String mac   = cols[3].toUpperCase();

                if (mac.equals("00:00:00:00:00:00")) continue;
                if (flags.equals("0x0"))             continue;

                result.put(mac, ip);
            }
        } catch (IOException e) {
            System.err.println("[NetworkUtil] Fehler beim Lesen von /proc/net/arp: " + e.getMessage());
        }
        return result;
    }

    // -------------------------------------------------------------------------

    /** Sendet einen einzelnen Ping an die IP (fire-and-forget). */
    private static void pingIp(String ip) {
        try {
            ProcessBuilder pb = new ProcessBuilder("ping", "-c", "1", "-W",
                    String.valueOf(PING_TIMEOUT_S), ip);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                while (br.readLine() != null) { /* Ausgabe verwerfen */ }
            }
            p.waitFor(PING_TIMEOUT_S + 1L, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException ignored) { }
    }

    // -------------------------------------------------------------------------

    /**
     * Liefert die erste nicht-loopback IPv4-Adresse des Hosts.
     * Funktioniert auf Linux/Raspberry Pi OS zuverlässig.
     */
    private static String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;

                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[NetworkUtil] Fehler beim Ermitteln der lokalen IP: " + e.getMessage());
        }
        return null;
    }

    // -------------------------------------------------------------------------

    /** Schneller eigenständiger Test ohne das restliche Projekt. */
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== NetworkUtil Standalone-Test ===");
        String localIp = getLocalIpAddress();
        System.out.println("Lokale IP:     " + localIp);

        System.out.println("Starte Scan...");
        refresh();
        System.out.println("Ergebnis:      " + cachedPersons);
    }
}
