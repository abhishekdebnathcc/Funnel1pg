package com.funnel1pg.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {

    private static final Properties properties = new Properties();

    static {
        try (InputStream input = ConfigReader.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input == null) throw new RuntimeException("config.properties not found");
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    public static String get(String key) {
        return System.getProperty(key, properties.getProperty(key, "")).trim();
    }

    public static String getBaseUrl()            { return get("base.url"); }
    public static String getBrowser()            { return get("browser"); }
    public static boolean isHeadless()           { return Boolean.parseBoolean(get("headless")); }
    public static int getSlowMo()                { return Integer.parseInt(get("slow.mo")); }
    public static int getTimeout()               { return Integer.parseInt(get("timeout")); }
    public static boolean isScreenshotOnFailure(){ return Boolean.parseBoolean(get("screenshot.on.failure")); }
    public static String getReportsDir()         { return get("reports.dir"); }
}
