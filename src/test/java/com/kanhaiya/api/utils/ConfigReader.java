package com.kanhaiya.api.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads values from config.properties so URLs and credentials are not hard-coded in tests.
 * Any value can be overridden from the command line, e.g. mvn test -DbaseUrl=https://other-env
 */
public class ConfigReader {

    private static final Properties properties = new Properties();

    // static block runs once when the class is first used, loading the file
    static {
        try (InputStream input = ConfigReader.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("config.properties not found in src/test/resources");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    // Returns the value for a key, e.g. get("baseUrl").
    // Order: -Dkey=value from the command line first, then config.properties.
    public static String get(String key) {
        String value = System.getProperty(key);             // command-line override
        if (value == null || value.isBlank()) {
            value = properties.getProperty(key);            // fall back to the file
        }
        if (value == null || value.isBlank()) {
            // Fail fast with a clear message instead of a NullPointerException later
            throw new IllegalStateException("Missing config value for '" + key
                    + "'. Add it to config.properties or pass -D" + key + "=...");
        }
        return value.trim();
    }
}
