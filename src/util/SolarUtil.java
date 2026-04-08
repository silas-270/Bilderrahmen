package util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import config.ConfigManager;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SolarUtil {

    private static final Gson gson = new Gson();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static SolarData cachedSolarData = new SolarData(0.0, 0.0, new double[8]);
    private static long lastFetchTime = 0;
    private static final int REFRESH_INTERVAL_MS = 60000; // 1 Minute

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

    private static class EntityState {
        String state;
    }

    public static SolarData fetchSolarData() {
        long now = System.currentTimeMillis();
        if (now - lastFetchTime > REFRESH_INTERVAL_MS) {
            refreshSolarData();
        }
        return cachedSolarData;
    }

    private static void refreshSolarData() {
        try {
            ConfigManager config = ConfigManager.load();
            
            // 1. Aktuelle Leistung (Power) abrufen
            double currentP = 0;
            try {
                currentP = fetchSingleValue(config, config.getHaEntityCurrentPower());
            } catch (Exception e) {
                System.err.println("[SolarUtil] Fehler bei Power-Sensor: " + e.getMessage());
            }

            // 2. Tagesertrag (Energy) abrufen
            double dailyE = 0;
            try {
                dailyE = fetchSingleValue(config, config.getHaEntityDailyEnergy());
            } catch (Exception e) {
                System.err.println("[SolarUtil] Fehler bei Energy-Sensor: " + e.getMessage());
            }

            // 3. Historie abrufen
            double[] history = new double[8];
            try {
                history = fetchHistory(config, config.getHaEntityCurrentPower(), 8);
            } catch (Exception e) {
                System.err.println("[SolarUtil] Fehler bei Historie: " + e.getMessage());
            }

            cachedSolarData = new SolarData(currentP, dailyE, history);
            lastFetchTime = System.currentTimeMillis();
        } catch (Exception e) {
            System.err.println("[SolarUtil] KRITISCHER FEHLER beim Update: " + e.getMessage());
        }
    }

    private static double fetchSingleValue(ConfigManager config, String entityId) throws Exception {
        String json = sendGetRequest(config, "states/" + entityId);
        EntityState entity = gson.fromJson(json, EntityState.class);
        return parseDouble(entity.state);
    }

    private static double[] fetchHistory(ConfigManager config, String entityId, int hours) throws Exception {
        String startTime = Instant.now()
                .minus(Duration.ofHours(hours))
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT);

        String json = sendGetRequest(config, "history/period/" + startTime + "?filter_entity_id=" + entityId);

        Type listType = new TypeToken<List<List<EntityState>>>() {
        }.getType();
        List<List<EntityState>> historyData = gson.fromJson(json, listType);

        if (historyData == null || historyData.isEmpty() || historyData.get(0).isEmpty()) {
            return new double[hours];
        }

        List<EntityState> states = historyData.get(0);
        double[] result = new double[hours];
        if (states.size() >= hours) {
            for (int i = 0; i < hours; i++) {
                int index = (int) ((double) i / (hours - 1) * (states.size() - 1));
                result[i] = parseDouble(states.get(index).state);
            }
        } else {
            for (int i = 0; i < states.size(); i++) {
                result[i] = parseDouble(states.get(i).state);
            }
        }
        
        return result;
    }

    private static String sendGetRequest(ConfigManager config, String endpoint) throws Exception {
        String fullUrl = config.getHaUrl() + endpoint;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("Authorization", "Bearer " + config.getHaToken())
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
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0.0;
        }
    }
}