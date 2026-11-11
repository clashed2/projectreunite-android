package com.example.projectreunite;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class ApiClient {
    // Pointing to your new backend API
    public static final String BASE_URL = "https://reunite.gethermedel.dev/";

    private ApiClient() {}

    public static JSONObject post(String endpoint, JSONObject body) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(BASE_URL + endpoint);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);
            connection.setDoOutput(true);

            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10)");

            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(bytes);
            }

            return readResponse(connection);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    public static JSONObject get(String endpoint) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(BASE_URL + endpoint);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10)");
            return readResponse(connection);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    public static JSONObject patch(String endpoint, JSONObject body) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(BASE_URL + endpoint);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PATCH");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);
            connection.setDoOutput(true);

            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10)");

            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(bytes);
            }

            return readResponse(connection);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static JSONObject readResponse(HttpURLConnection connection) throws Exception {
        int status = connection.getResponseCode();
        InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();

        StringBuilder result = new StringBuilder();
        if (stream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) result.append(line);
            }
        }

        if (result.length() == 0) throw new Exception("Server returned HTTP " + status + " with no content");

        try {
            return new JSONObject(result.toString());
        } catch (org.json.JSONException e) {
            String snippet = result.length() > 100 ? result.substring(0, 100) : result.toString();
            throw new Exception("Invalid Response (HTTP " + status + "): " + snippet);
        }
    }

    public static class ApiException extends Exception {
        public final int statusCode;
        public ApiException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }
    }
}