package lk.com.synsoft.offlinepos.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application settings.
 *
 * Read in two passes: the defaults bundled in the jar, then an optional file in
 * the user's data folder that overrides any of them. That is what lets one
 * installer serve every shop — the till's own database name, port and
 * credentials live outside the build.
 *
 * The legacy app hardcoded root with an empty password in Includes/config.php,
 * and the JavaFX scaffold this replaces hardcoded root/1234. Neither is
 * something you can hand to a shop.
 */
public final class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
    private static final String BUNDLED = "/offlinepos.properties";

    private static AppConfig instance;

    private final Properties props;
    private final Path overrideFile;
    private final boolean overrideLoaded;

    private AppConfig(Properties props, Path overrideFile, boolean overrideLoaded) {
        this.props = props;
        this.overrideFile = overrideFile;
        this.overrideLoaded = overrideLoaded;
    }

    public static synchronized AppConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static AppConfig load() {
        Properties defaults = new Properties();

        try (InputStream in = AppConfig.class.getResourceAsStream(BUNDLED)) {
            if (in == null) {
                throw new IllegalStateException(
                        "Bundled " + BUNDLED + " is missing from the build.");
            }
            defaults.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read bundled settings.", e);
        }

        Properties merged = new Properties();
        merged.putAll(defaults);

        Path override = AppPaths.configFile();
        boolean loaded = false;

        if (Files.isReadable(override)) {
            Properties local = new Properties();
            try (Reader r = Files.newBufferedReader(override, StandardCharsets.UTF_8)) {
                local.load(r);
                merged.putAll(local);
                loaded = true;
            } catch (IOException e) {
                // Falling back to defaults beats refusing to start: the user can
                // still reach the settings screen and fix the file.
                log.warn("Could not read {}, using bundled defaults.", override, e);
            }
        }

        return new AppConfig(merged, override, loaded);
    }

    // ---------- database ----------

    public String dbHost() {
        return required("db.host");
    }

    public int dbPort() {
        return intValue("db.port", 3306);
    }

    public String dbName() {
        return required("db.name");
    }

    public String dbUser() {
        return required("db.user");
    }

    public String dbPassword() {
        return props.getProperty("db.password", "");
    }

    public int dbPoolSize() {
        return intValue("db.pool.size", 8);
    }

    /**
     * The JDBC URL. useSSL is off because this is a local or shop-LAN server,
     * and allowPublicKeyRetrieval keeps MySQL 8's default auth plugin working
     * on a plain connection.
     */
    public String jdbcUrl() {
        return "jdbc:mysql://" + dbHost() + ":" + dbPort() + "/" + dbName()
                + "?useSSL=false"
                + "&allowPublicKeyRetrieval=true"
                + "&serverTimezone=" + value("db.timezone", "Asia/Colombo")
                + "&characterEncoding=UTF-8";
    }

    // ---------- application ----------

    public String appName() {
        return value("app.name", "OfflinePOS");
    }

    public String appVersion() {
        return value("app.version", "0.0.0");
    }

    public boolean devMode() {
        return Boolean.parseBoolean(value("app.devMode", "false"));
    }

    // ---------- diagnostics ----------

    public Path overrideFile() {
        return overrideFile;
    }

    public boolean overrideLoaded() {
        return overrideLoaded;
    }

    /** Safe to log: the password is never included. */
    public String describe() {
        return "db=" + dbUser() + "@" + dbHost() + ":" + dbPort() + "/" + dbName()
                + ", pool=" + dbPoolSize()
                + ", settings=" + (overrideLoaded ? overrideFile : "bundled defaults");
    }

    // ---------- helpers ----------

    private String value(String key, String fallback) {
        String v = props.getProperty(key);
        return (v == null || v.isBlank()) ? fallback : v.trim();
    }

    private String required(String key) {
        String v = props.getProperty(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException(
                    "Setting '" + key + "' is missing. Add it to " + overrideFile + ".");
        }
        return v.trim();
    }

    private int intValue(String key, int fallback) {
        String v = props.getProperty(key);
        if (v == null || v.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            log.warn("Setting '{}' is not a number ('{}'), using {}.", key, v, fallback);
            return fallback;
        }
    }
}
