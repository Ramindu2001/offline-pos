package lk.com.synsoft.offlinepos.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Where the application keeps everything that is not part of the install:
 * logs, the local configuration override, and later the database backups.
 *
 * Nothing writable ever lives beside the executable. On Windows the install
 * folder is usually under Program Files, which a cashier account cannot write
 * to, so all of it goes under the user's application data folder instead.
 *
 * This class must not log. It is used to work out where the log file goes,
 * so it runs before logging exists.
 */
public final class AppPaths {

    private static final String APP_FOLDER = "OfflinePOS";

    private AppPaths() {
    }

    /**
     * The root data folder, created if it is not there yet.
     * Windows: %APPDATA%\OfflinePOS   Anywhere else: ~/.offlinepos
     */
    public static Path dataDir() {
        String appData = System.getenv("APPDATA");

        Path root = (appData != null && !appData.isBlank())
                ? Paths.get(appData, APP_FOLDER)
                : Paths.get(System.getProperty("user.home"), "." + APP_FOLDER.toLowerCase());

        return ensure(root);
    }

    /** Rolling application logs. The only field diagnostic once this ships. */
    public static Path logDir() {
        return ensure(dataDir().resolve("logs"));
    }

    /**
     * The optional local override file. It is not created by the installer:
     * the bundled defaults apply until someone writes one.
     */
    public static Path configFile() {
        return dataDir().resolve("offlinepos.properties");
    }

    /** Database backups. Read from Phase 15 onwards. */
    public static Path backupDir() {
        return ensure(dataDir().resolve("backups"));
    }

    private static Path ensure(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            // A read-only data folder is fatal, but this class cannot log yet
            // and must not kill the launch on its own. Fall back to the working
            // directory so the app can still start and report the problem.
            return Paths.get(".");
        }
        return dir;
    }
}
