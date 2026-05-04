package util;

import com.google.gson.Gson;
import config.ConfigManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class SolarUtil {

    private static final Gson gson = new Gson();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static double lastLivePower = 0;
    private static double lastDailyEnergy = 0;
    private static double[] lastHistory = new double[0];

    private static long lastLiveFetch = 0;
    private static long lastSummaryFetch = 0;
    private static long lastGraphFetch = 0;

    private static final long LIVE_INTERVAL = 3 * 60 * 1000; // 3 min
    private static final long SUMMARY_INTERVAL = 10 * 60 * 1000; // 10 min
    private static final long GRAPH_INTERVAL = 20 * 60 * 1000; // 20 min

    public static class SolarData {
        private final double currentPower;
        private final double dailyEnergy;
        private final double[] history;

        public SolarData(double currentPower, double dailyEnergy, double[] history) {
            this.currentPower = currentPower;
            this.dailyEnergy = dailyEnergy;
            this.history = history;
        }

        public double getCurrentPower() {
            return currentPower;
        }

        public double getDailyEnergy() {
            return dailyEnergy;
        }

        public double[] getHistory() {
            return history;
        }
    }

    // --- JSON Mapping Classes ---

    private static class HaState {
        String state;
    }

    private static class HaHistoryPoint {
        String state;
        String last_changed;
    }

    // --- Logic ---

    public static SolarData fetchSolarData() {
        refreshSolarData();
        return new SolarData(lastLivePower, lastDailyEnergy, lastHistory);
    }

    private static void refreshSolarData() {
        try {
            ConfigManager config = ConfigManager.load();
            String host = config.getHaIp();
            String token = config.getHaToken();
            
            if (host == null || host.isEmpty() || token == null || token.isEmpty()) {
                System.err.println("[SolarUtil] HA IP oder Token nicht konfiguriert.");
                return;
            }

            String baseUrl = "http://" + host + ":8123/api";
            long now = System.currentTimeMillis();

            // 1. Summary abrufen (Daily Energy) - Alle 10 min
            if (now - lastSummaryFetch > SUMMARY_INTERVAL) {
                try {
                    String summaryJson = sendGetRequest(baseUrl + "/states/sensor.wechselrichter_yieldday", token);
                    HaState stateObj = gson.fromJson(summaryJson, HaState.class);
                    if (stateObj != null && stateObj.state != null) {
                        lastDailyEnergy = parseDouble(stateObj.state) / 1000.0;
                    }
                    lastSummaryFetch = now;
                } catch (Exception e) {
                    System.err.println("[SolarUtil] Fehler bei Summary (Yield): " + e.getMessage());
                }
            }

            // 2. Live abrufen (Current Power) - Alle 3 min
            if (now - lastLiveFetch > LIVE_INTERVAL) {
                try {
                    String liveJson = sendGetRequest(baseUrl + "/states/sensor.wechselrichter_power", token);
                    HaState stateObj = gson.fromJson(liveJson, HaState.class);
                    if (stateObj != null && stateObj.state != null) {
                        lastLivePower = parseDouble(stateObj.state);
                    }
                    lastLiveFetch = now;
                } catch (Exception e) {
                    System.err.println("[SolarUtil] Fehler bei Live (Power): " + e.getMessage());
                }
            }

            // 3. Graph abrufen (Historie) - Alle 20 min
            int hours = config.getSolarHistoryHours();
            int intervalMinutes = config.getSolarHistoryInterval();
            int expectedPoints = (hours * 60) / intervalMinutes;

            if (now - lastGraphFetch > GRAPH_INTERVAL || lastHistory.length != expectedPoints) {
                try {
                    ZonedDateTime startTimeZdt = ZonedDateTime.now().minus(hours, ChronoUnit.HOURS);
                    String startTimeStr = startTimeZdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    
                    String url = baseUrl + "/history/period/" + startTimeStr + "?filter_entity_id=sensor.wechselrichter_power";
                    String graphJson = sendGetRequest(url, token);
                    
                    HaHistoryPoint[][] historyArray = gson.fromJson(graphJson, HaHistoryPoint[][].class);
                    
                    if (historyArray != null && historyArray.length > 0 && historyArray[0] != null) {
                        lastHistory = parseHistory(historyArray[0], hours, expectedPoints, intervalMinutes);
                    } else {
                        lastHistory = new double[expectedPoints];
                    }
                    lastGraphFetch = now;
                } catch (Exception e) {
                    System.err.println("[SolarUtil] Fehler bei Historie: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("[SolarUtil] KRITISCHER FEHLER beim Update: " + e.getMessage());
        }
    }

    private static double[] parseHistory(HaHistoryPoint[] points, int hours, int expectedPoints, int intervalMinutes) {
        double[] result = new double[expectedPoints];
        
        long endMillis = System.currentTimeMillis();
        long startMillis = endMillis - (hours * 3600000L);
        long intervalMillis = intervalMinutes * 60000L;
        
        int pointIdx = 0;
        int maxPoints = points.length;
        double currentVal = 0.0;
        
        for (int i = 0; i < expectedPoints; i++) {
            long bucketEnd = startMillis + (i + 1) * intervalMillis;
            
            while (pointIdx < maxPoints) {
                HaHistoryPoint p = points[pointIdx];
                long t = ZonedDateTime.parse(p.last_changed).toInstant().toEpochMilli();
                
                if (t <= bucketEnd) {
                    currentVal = parseDouble(p.state);
                    pointIdx++;
                } else {
                    break;
                }
            }
            result[i] = currentVal;
        }
        
        return result;
    }

    private static String sendGetRequest(String url, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP Fehler: " + response.statusCode());
        }
        return response.body();
    }

    private static double parseDouble(String value) {
        if (value == null || value.equals("unavailable") || value.equals("unknown"))
            return 0.0;
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0.0;
        }
    }

    // --- Test Main ---
    public static void main(String[] args) {
        System.out.println("Teste Home Assistant Verbindung...");
        
        // Erzwinge den ersten Abruf
        SolarData data = fetchSolarData();
        
        System.out.println("\n--- Erwartetes Format ---");
        System.out.println("Die Werte sollten numerisch sein (z.B. 123.4).");
        System.out.println("Die Historie ist ein Array mit " + data.getHistory().length + " Werten (entsprechend der konfigurierten Stunden und Intervalle).");
        
        System.out.println("\n--- Tatsächliche Ergebnisse ---");
        System.out.println("Aktuelle Leistung (Live): " + data.getCurrentPower() + " W");
        System.out.println("Tagesertrag (Summary): " + data.getDailyEnergy() + " kWh");
        
        System.out.println("Historie (Graph):");
        double[] history = data.getHistory();
        for (int i = 0; i < history.length; i++) {
            System.out.println("  Bucket " + i + ": " + history[i]);
        }
        System.out.println("-------------------------");
    }
}