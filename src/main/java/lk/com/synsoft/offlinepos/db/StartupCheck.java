package lk.com.synsoft.offlinepos.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.sql.DataSource;

import lk.com.synsoft.offlinepos.config.AppPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * What the application proves about itself before it opens a window.
 *
 * A till has no server behind it and nobody to call. When something is wrong it
 * has to say which thing, in one line, on screen - not fail somewhere in the
 * middle of the first sale of the day. So the three things everything else
 * depends on are checked up front, in order, and a failure stops the ones that
 * follow rather than producing a second, misleading error.
 *
 *   1. The data folder can be written to - otherwise there are no logs and no
 *      backups, and the only field diagnostic this build has is gone.
 *   2. The database answers, and it is the server we think it is.
 *   3. The schema is at the version this build was written against.
 */
public final class StartupCheck {

    private static final Logger log = LoggerFactory.getLogger(StartupCheck.class);

    public enum Status { PASS, FAIL, SKIPPED }

    /** One line of the report. {@code detail} is shown to the user as-is. */
    public record Check(String name, Status status, String detail) {

        public boolean failed() {
            return status == Status.FAIL;
        }
    }

    public record Report(List<Check> checks, int migrationsApplied) {

        public boolean ok() {
            return checks.stream().noneMatch(Check::failed);
        }

        public Optional<Check> failure() {
            return checks.stream().filter(Check::failed).findFirst();
        }
    }

    private final DataSource dataSource;

    public StartupCheck(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Report run() {
        List<Check> checks = new ArrayList<>();

        Check disk = checkDataFolder();
        checks.add(disk);

        Check database = checkDatabase();
        checks.add(database);

        if (database.failed()) {
            checks.add(new Check("Schema", Status.SKIPPED, "Not checked - the database is unreachable."));
            report(checks);
            return new Report(List.copyOf(checks), 0);
        }

        int applied = 0;
        Check schema;
        try {
            MigrationRunner runner = new MigrationRunner(dataSource);
            applied = runner.migrate();

            List<String> pending = runner.pending();
            schema = pending.isEmpty()
                    ? new Check("Schema", Status.PASS, describeSchema(applied))
                    : new Check("Schema", Status.FAIL,
                                "The database is behind this version of the program: "
                                + String.join(", ", pending) + " did not apply.");

        } catch (MigrationException e) {
            log.error("Schema check failed.", e);
            schema = new Check("Schema", Status.FAIL, e.getMessage());
        }
        checks.add(schema);

        report(checks);
        return new Report(List.copyOf(checks), applied);
    }

    // ------------------------------------------------------------------

    /**
     * Writes and deletes a probe file. Being able to create the folder is not
     * the same as being able to write in it - a roaming profile or a locked-down
     * Program Files install will happily report the directory and then refuse
     * the file.
     */
    private Check checkDataFolder() {
        Path dataDir = AppPaths.dataDir();
        Path probe = dataDir.resolve(".write-test");

        try {
            Files.writeString(probe, "ok");
            Files.deleteIfExists(probe);
            return new Check("Data folder", Status.PASS, dataDir.toString());

        } catch (IOException e) {
            log.error("Data folder is not writable: {}", dataDir, e);
            return new Check("Data folder", Status.FAIL,
                    "Cannot write to " + dataDir + ". Logs and backups will not be saved.");
        }
    }

    /**
     * Opens one connection and looks at what answered.
     *
     * The server identity is not a formality here. Two MySQL-family servers run
     * on a typical development machine - MySQL 8 on 3307 and XAMPP's MariaDB
     * 10.4 on 3306 holding the legacy database - and a mistyped port would point
     * a fresh install at the reference data instead of its own. MariaDB also
     * accepts SQL that MySQL 8 rejects and has no utf8mb4_0900_ai_ci, so the
     * migrations would half-apply.
     */
    private Check checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();

            String product = meta.getDatabaseProductName();
            String version = meta.getDatabaseProductVersion();
            String url = meta.getURL();

            if (looksLikeMariaDb(product, version)) {
                log.error("Connected to MariaDB at {} - this build targets MySQL 8.", url);
                return new Check("Database", Status.FAIL,
                        "The server at this address is MariaDB (" + version + "), not MySQL 8. "
                        + "Check db.port in the settings file - MySQL is usually on 3307 here, "
                        + "and 3306 is the legacy database.");
            }

            if (meta.getDatabaseMajorVersion() < 8) {
                return new Check("Database", Status.FAIL,
                        "MySQL " + version + " is too old. This build needs MySQL 8 or later.");
            }

            return new Check("Database", Status.PASS, product + " " + version);

        } catch (SQLException e) {
            log.error("Database is unreachable.", e);
            return new Check("Database", Status.FAIL,
                    "Cannot reach the database. Check that the MySQL service is running.");
        }
    }

    /**
     * MariaDB answers the MySQL protocol and reports its product name as
     * "MySQL", so the only thing that tells the two apart is the version string
     * - "10.4.32-MariaDB" against "8.0.46". Package-private so the branch that
     * matters most can be tested without a second server running.
     */
    static boolean looksLikeMariaDb(String product, String version) {
        return (product + " " + version).toLowerCase(Locale.ROOT).contains("mariadb");
    }

    private String describeSchema(int applied) {
        return applied == 0
                ? "Up to date."
                : "Brought up to date - applied " + applied + " migration(s).";
    }

    private void report(List<Check> checks) {
        for (Check check : checks) {
            if (check.failed()) {
                log.error("Startup check {}: FAILED - {}", check.name(), check.detail());
            } else {
                log.info("Startup check {}: {} - {}", check.name(), check.status(), check.detail());
            }
        }
    }
}
