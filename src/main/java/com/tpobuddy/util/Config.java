package com.tpobuddy.util;

import java.io.InputStream;
import java.util.Properties;

/**
 * Global Configuration loader.
 * Reads application.properties with fallback to environment variables.
 */
public class Config {
    private static final Properties props = new Properties();

    static {
        try (InputStream in = Config.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not load application.properties, using defaults.");
        }
    }

    public static String get(String key, String defaultValue) {
        String envKey1 = key.replace('.', '_').toUpperCase();
        String envVal = System.getenv(envKey1);
        if (envVal != null && !envVal.trim().isEmpty()) {
            return envVal.trim();
        }
        String envKey2 = key.replaceAll("([a-z])([A-Z]+)", "$1_$2").replace('.', '_').toUpperCase();
        envVal = System.getenv(envKey2);
        if (envVal != null && !envVal.trim().isEmpty()) {
            return envVal.trim();
        }
        return props.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static String getFirebaseProjectId() {
        return get("firebase.projectId", "tpobuddy");
    }

    public static String getFirebaseApiKey() {
        return get("firebase.apiKey", "");
    }

    public static String getFirebaseDatabaseUrl() {
        return get("firebase.databaseURL", "https://tpobuddy-default-rtdb.firebaseio.com");
    }

    public static String getFirebaseStorageBucket() {
        return get("firebase.storageBucket", "tpobuddy.firebasestorage.app");
    }

    public static String getFirebaseAppId() {
        return get("firebase.appId", "1:811364970356:web:09bc29db4bff3a07cb4621");
    }

    public static String getFirebaseMeasurementId() {
        return get("firebase.measurementId", "G-K3KP8NTCSF");
    }
}
