package util;

import com.google.gson.Gson;
import config.ConfigManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

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

    private static class SummaryResponse {
        SummaryData data;
    }

    private static class SummaryData {
        String today_eq;
        String real_power;
    }

    private static class LiveResponse {
        LiveData data;
    }

    private static class LiveData {
        LivePower power;
    }

    private static class LivePower {
        double pv;
    }

    private static class RecentResponse {
        // String range;
        // int requested_hours;
        // int interval_minutes;
        List<RecentPoint> data;
    }

    private static class RecentPoint {
        // String time;
        // long timestamp;
        double power;
        // double voltage;
        // double frequency;
        // double temperature;
    }

    // --- Logic ---

    public static SolarData fetchSolarData() {
        refreshSolarData();
        return new SolarData(lastLivePower, lastDailyEnergy, lastHistory);
    }

    private static void refreshSolarData() {
        try {
            ConfigManager config = ConfigManager.load();
            String baseUrl = config.getSolarBaseUrl();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }

            long now = System.currentTimeMillis();

            // 1. Summary abrufen (Daily Energy & Fallback Power) - Alle 10 min
            if (now - lastSummaryFetch > SUMMARY_INTERVAL) {
                try {
                    String summaryJson = sendGetRequest(baseUrl + "/api/summary");
                    SummaryResponse summary = gson.fromJson(summaryJson, SummaryResponse.class);
                    if (summary != null && summary.data != null) {
                        lastDailyEnergy = parseDouble(summary.data.today_eq) / 1000.0;
                        // Wir speichern die real_power nur als Fallback, falls live.power.pv 0 ist
                        if (lastLivePower == 0) {
                            lastLivePower = parseDouble(summary.data.real_power);
                        }
                    }
                    lastSummaryFetch = now;
                } catch (Exception e) {
                    System.err.println("[SolarUtil] Fehler bei /api/summary: " + e.getMessage());
                }
            }

            // 2. Live abrufen (Current Power) - Alle 3 min
            if (now - lastLiveFetch > LIVE_INTERVAL) {
                try {
                    String liveJson = sendGetRequest(baseUrl + "/api/live");
                    LiveResponse live = gson.fromJson(liveJson, LiveResponse.class);
                    if (live != null && live.data != null && live.data.power != null) {
                        if (live.data.power.pv > 0) {
                            lastLivePower = live.data.power.pv;
                        }
                    }
                    lastLiveFetch = now;
                } catch (Exception e) {
                    System.err.println("[SolarUtil] Fehler bei /api/live: " + e.getMessage());
                }
            }

            // 3. Graph abrufen (Historie) - Alle 20 min
            int hours = config.getSolarHistoryHours();
            int interval = config.getSolarHistoryInterval();
            int expectedPoints = (hours * 60) / interval;

            if (now - lastGraphFetch > GRAPH_INTERVAL || lastHistory.length != expectedPoints) {
                try {
                    String url = String.format("%s/api/recent?hours=%d&interval=%d", baseUrl, hours, interval);
                    String graphJson = sendGetRequest(url);
                    RecentResponse recent = gson.fromJson(graphJson, RecentResponse.class);

                    if (recent != null && recent.data != null) {
                        List<RecentPoint> points = recent.data;
                        double[] history = new double[points.size()];
                        for (int i = 0; i < points.size(); i++) {
                            history[i] = points.get(i).power;
                        }
                        lastHistory = history;
                    }
                    lastGraphFetch = now;
                } catch (Exception e) {
                    System.err.println("[SolarUtil] Fehler bei /api/recent: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("[SolarUtil] KRITISCHER FEHLER beim Update: " + e.getMessage());
        }
    }

    private static String sendGetRequest(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
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
        if (value == null)
            return 0.0;
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0.0;
        }
    }
}