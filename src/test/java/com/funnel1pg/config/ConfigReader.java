package com.funnel1pg.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * ConfigReader – loads config.properties and honours command-line overrides.
 *
 * base.url and select.all.products overrides are persisted back to the SOURCE
 * config.properties in src/test/resources/ so the next run picks them up
 * without needing the -D flag again.
 *
 * Usage:
 *   First run with new URL:       mvn test -Dbase.url=https://example.com/myFunnel/
 *   All subsequent runs:          mvn test          ← uses saved URL automatically
 *
 *   Override email:               mvn test -Demail=me@example.com
 *   Select all products:          mvn test -Dselect.all.products=true
 *   Disable select all products:  mvn test -Dselect.all.products=false
 */
public class ConfigReader {

    private static final String CONFIG_FILE = "config.properties";
    private static final Properties properties = new Properties();

    /**
     * Resolved path to the SOURCE config file (src/test/resources/config.properties).
     * We walk up from target/test-classes back to the project root, then resolve the
     * source path. This is the file we persist changes to.
     */
    private static Path configFilePath;

    static {
        // 1. Load from classpath (target/test-classes copy, always up-to-date for reading)
        try (InputStream input = ConfigReader.class.getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {
            if (input == null) throw new RuntimeException("config.properties not found on classpath");
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }

        // 2. Locate the SOURCE file on disk for writing
        try {
            java.net.URL classpathUrl = ConfigReader.class.getClassLoader().getResource(CONFIG_FILE);
            if (classpathUrl != null) {
                Path classpathFile = Paths.get(classpathUrl.toURI());
                // classpathFile = .../target/test-classes/config.properties
                // Walk up: target/test-classes -> target -> project-root
                // Then descend: src/test/resources/config.properties
                Path projectRoot = classpathFile.getParent().getParent().getParent();
                Path sourceFile  = projectRoot.resolve("src/test/resources/" + CONFIG_FILE);
                if (Files.exists(sourceFile)) {
                    configFilePath = sourceFile;
                    System.out.println("[ConfigReader] Source config: " + configFilePath);
                } else {
                    System.out.println("[ConfigReader] WARNING: source config not found at " + sourceFile);
                    configFilePath = classpathFile; // fallback to target copy
                }
            }
        } catch (Exception e) {
            System.out.println("[ConfigReader] WARNING: could not resolve source config path: " + e.getMessage());
        }

        // 3. Apply and persist any -D overrides
        applyAndPersistIfChanged("base.url");
        applyAndPersistIfChanged("select.all.products");
    }

    /**
     * If a system property (-D flag) was passed for this key and differs from the
     * current value in the file, update the in-memory properties AND write back to
     * the source file so future runs use the new value by default.
     */
    private static void applyAndPersistIfChanged(String key) {
        String sysProp = System.getProperty(key);
        if (sysProp == null || sysProp.trim().isEmpty()) return;
        sysProp = sysProp.trim();
        String current = properties.getProperty(key, "").trim();
        if (!sysProp.equals(current)) {
            properties.setProperty(key, sysProp);
            System.out.println("[ConfigReader] Persisting " + key + " = " + sysProp);
            persist();
        }
    }

    /**
     * Writes current properties back to the source config file, preserving all
     * comments and the existing key order. Only the changed key values are updated.
     */
    private static void persist() {
        if (configFilePath == null) {
            System.out.println("[ConfigReader] WARNING: cannot persist – source file path unknown");
            return;
        }
        try {
            java.util.List<String> lines = Files.readAllLines(configFilePath);
            java.util.List<String> updated = new java.util.ArrayList<>();
            java.util.Set<String> written  = new java.util.HashSet<>();

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("#") || trimmed.isEmpty()) {
                    updated.add(line);
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq > 0) {
                    String k = line.substring(0, eq).trim();
                    if (properties.containsKey(k)) {
                        updated.add(k + "=" + properties.getProperty(k));
                        written.add(k);
                        continue;
                    }
                }
                updated.add(line);
            }
            // Append any brand-new keys not previously in the file
            for (String k : properties.stringPropertyNames()) {
                if (!written.contains(k)) {
                    updated.add(k + "=" + properties.getProperty(k));
                }
            }
            Files.write(configFilePath, updated);
            System.out.println("[ConfigReader] config.properties saved -> " + configFilePath);
        } catch (IOException e) {
            System.out.println("[ConfigReader] WARNING: could not persist config: " + e.getMessage());
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** System property wins over file value. */
    public static String get(String key) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.trim().isEmpty()) return sys.trim();
        return properties.getProperty(key, "").trim();
    }

    /** Base URL – always ends with /  e.g. https://example.com/myFunnel/ */
    public static String getBaseUrl() {
        String url = get("base.url");
        return url.endsWith("/") ? url : url + "/";
    }

    /** Email override – only from -Demail= flag, never read from or written to config.properties */
    public static String getEmailOverride() {
        String v = System.getProperty("email", "").trim();
        return v.isEmpty() ? null : v;
    }

    /** Whether to select all available products instead of just the first. */
    public static boolean isSelectAllProducts() {
        return Boolean.parseBoolean(get("select.all.products"));
    }

    public static String getBrowser()             { return get("browser"); }
    public static boolean isHeadless()            { return Boolean.parseBoolean(get("headless")); }
    public static int getSlowMo()                 { return Integer.parseInt(get("slow.mo")); }
    public static int getTimeout()                { return Integer.parseInt(get("timeout")); }
    public static boolean isScreenshotOnFailure() { return Boolean.parseBoolean(get("screenshot.on.failure")); }
    public static String getReportsDir()          { return get("reports.dir"); }

    public static String getPaymentMethod() {
        String v = get("payment.method");
        return (v == null || v.isEmpty()) ? "creditcard" : v.toLowerCase();
    }
}
