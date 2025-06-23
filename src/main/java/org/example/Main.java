package org.example;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class Main {
    private static final String IPWHOIS_URL = "https://ipwho.is/";
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        try {
            String pageTitle = "Java";
            String json = fetchRevisionsJson(pageTitle);
            Set<String> ips = extractIPs(json);
            System.out.println("Найдено уникальных IP-адресов: " + ips.size());
            Map<String, Integer> countryCount = countEditorsByCountry(ips);
            List<Entry<String, Integer>> ranking = sortByEditorCount(countryCount);
            displayCountryRanking(ranking);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Получение JSON-строки с ревизиями страницы
    private static String fetchRevisionsJson(String title) throws Exception {
        String encoded = URLEncoder.encode(title, "UTF-8");
        String urlStr = "https://en.wikipedia.org/w/api.php" +
                "?action=query&prop=revisions&rvprop=user" +
                "&titles=" + encoded + "&rvlimit=max&format=json";
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("HTTP error: " + conn.getResponseCode());
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) sb.append(line);
        in.close();
        return sb.toString();
    }

    // Извлечение уникальных IP-адресов из JSON
    private static Set<String> extractIPs(String json) {
        Set<String> ipSet = new HashSet<>();
        JsonObject root = gson.fromJson(json, JsonObject.class);
        JsonObject pages = root.getAsJsonObject("query").getAsJsonObject("pages");
        for (JsonElement pageElem : pages.entrySet().stream().map(e -> e.getValue()).toList()) {
            JsonArray revs = pageElem.getAsJsonObject().getAsJsonArray("revisions");
            if (revs != null) {
                for (JsonElement rev : revs) {
                    String user = rev.getAsJsonObject().get("user").getAsString();
                    if (user.matches("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b")) {
                        ipSet.add(user);
                    }
                }
            }
        }
        return ipSet;
    }

    // Получение страны по IP-адресу
    private static String fetchCountry(String ip) throws Exception {
        String urlStr = IPWHOIS_URL + ip;
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            return "Unknown";
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) sb.append(line);
        in.close();
        JsonObject resp = gson.fromJson(sb.toString(), JsonObject.class);
        return resp.has("country") ? resp.get("country").getAsString() : "Unknown";
    }

    // Подсчёт количества редакторов по странам
    private static Map<String, Integer> countEditorsByCountry(Set<String> ips) throws Exception {
        Map<String, Integer> countMap = new HashMap<>();
        for (String ip : ips) {
            String country = fetchCountry(ip);
            countMap.put(country, countMap.getOrDefault(country, 0) + 1);
        }
        return countMap;
    }

    // Сортировка рейтинга стран по убыванию количества редакторов
    private static List<Entry<String, Integer>> sortByEditorCount(Map<String, Integer> countMap) {
        List<Entry<String, Integer>> list = new ArrayList<>(countMap.entrySet());
        list.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        return list;
    }

    // Вывод рейтинга стран
    private static void displayCountryRanking(List<Entry<String, Integer>> ranking) {
        System.out.println("Рейтинг стран по количеству уникальных IP-редакторов:");
        for (Entry<String, Integer> e : ranking) {
            System.out.println(e.getKey() + ": " + e.getValue());
        }
    }
}