package com.kanhaiya.api.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads values from config.properties so URLs and credentials are not hard-coded in tests.
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

    // Returns the value for a key, e.g. get("baseUrl")
    public static String get(String key) {
        return properties.getProperty(key);
    }
}
