package util;

import config.ConfigManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hilfsklasse zum Abrufen von Wetterdaten über die Open-Meteo API.
 */
public class WeatherUtil {

    private static WeatherData cachedWeather = new WeatherData("---", "not-available");
    private static long lastFetchTime = 0;

    public record WeatherData(String temperature, String iconName) {
    }

    public static WeatherData fetchCurrentWeather() {
        long now = System.currentTimeMillis();
        int intervalSeconds = ConfigManager.load().getWeatherRefreshIntervalSeconds();
        if (intervalSeconds < 5)
            intervalSeconds = 1800;

        if (now - lastFetchTime > intervalSeconds * 1000L) {
            refreshWeather();
        }
        return cachedWeather;
    }

    private static void refreshWeather() {
        System.out.println("[WeatherUtil] Update Wetter-Daten...");

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            ConfigManager config = ConfigManager.load();
            String url = String.format(Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current_weather=true",
                    config.getWeatherLatitude(), config.getWeatherLongitude());

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String json = response.body();

                // Extraktion der relevanten Felder
                double temp = extractValue(json, "\"temperature\":\\s*([\\d.-]+)");
                int code = (int) extractValue(json, "\"weathercode\":\\s*(\\d+)");
                int isDay = (int) extractValue(json, "\"is_day\":\\s*(\\d+)");

                String formattedTemp = Math.round(temp) + "°";

                // Mapping auf deine Icon-Collection
                String iconName = mapWmoCodeToIcon(code, isDay == 1);

                cachedWeather = new WeatherData(formattedTemp, iconName);
                lastFetchTime = System.currentTimeMillis();

                System.out.println("[WeatherUtil] Update fertig: " + formattedTemp + ", Icon: " + iconName);
            }
        } catch (Exception e) {
            System.err.println("[WeatherUtil] Fehler: " + e.getMessage());
        }
    }

    private static double extractValue(String json, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(json);
        if (matcher.find())
            return Double.parseDouble(matcher.group(1));
        return 0.0;
    }

    /**
     * Mappt WMO-Codes auf DEINE Icon-Collection.
     */
    private static String mapWmoCodeToIcon(int wmoCode, boolean isDay) {
        return switch (wmoCode) {
            case 0 -> isDay ? "clear-day" : "clear-night";
            case 1, 2 -> "partly-cloudy";
            case 3 -> "cloudy";
            case 45, 48 -> "fog";
            case 51, 53, 55 -> "rain-1"; // Nieselregen (leicht bis stark)
            case 56, 57, 61, 63 -> "rain-2"; // Gefrierender Niesel & mäßiger Regen
            case 65, 66, 67, 80, 81, 82 -> "rain-3"; // Starkregen & Regenschauer
            case 71, 73, 75, 77, 85, 86 -> "snow"; // Alle Schnee-Arten
            case 95 -> "thunderstorms-rain"; // Gewitter
            case 96, 99 -> "hail"; // Gewitter mit Hagel
            default -> "not-available";
        };
    }
}