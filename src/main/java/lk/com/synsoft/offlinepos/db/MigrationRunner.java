package lk.com.synsoft.offlinepos.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies the schema migrations listed in db/migration/migrations.index.
 *
 * There is no server to migrate a shop from, so this runs at every launch:
 * whatever version the till is on, it catches up before the UI opens.
 *
 * Two guarantees:
 *
 *   Applied once. Every migration is recorded in schema_version, and one that
 *   is already recorded is skipped.
 *
 *   Never changed after the fact. The checksum of each file is stored, and a
 *   mismatch stops the launch rather than leaving two shops on schemas that
 *   share a version number but differ.
 *
 * MySQL gives DDL no transactional protection - an ALTER commits whatever came
 * before it - so a migration cannot be rolled back as a unit. Each one is
 * therefore kept small and, where it can be, re-runnable.
 */
public final class MigrationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigrationRunner.class);

    private static final String LOCATION = "/db/migration/";
    private static final String INDEX = LOCATION + "migrations.index";

    private final DataSource dataSource;

    public MigrationRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** @return how many migrations were applied this run. */
    public int migrate() {
        try (Connection connection = dataSource.getConnection()) {
            createVersionTable(connection);

            Map<String, String> applied = loadApplied(connection);
            List<String> planned = readIndex();

            verifyNothingChanged(applied, planned);

            int count = 0;
            for (String name : planned) {
                if (applied.containsKey(name)) {
                    continue;
                }
                apply(connection, name);
                count++;
            }

            if (count == 0) {
                log.info("Schema is up to date ({} migrations already applied).", applied.size());
            } else {
                log.info("Applied {} migration(s). Schema now at {} total.", count, applied.size() + count);
            }
            return count;

        } catch (SQLException e) {
            throw new MigrationException("Could not bring the database up to date.", e);
        }
    }

    // ------------------------------------------------------------------

    private void createVersionTable(Connection connection) throws SQLException {
        String ddl = """
                CREATE TABLE IF NOT EXISTS `schema_version` (
                  `id`           int NOT NULL AUTO_INCREMENT,
                  `migration`    varchar(200) NOT NULL,
                  `checksum`     char(64)     NOT NULL,
                  `applied_at`   datetime     NOT NULL,
                  `duration_ms`  int          NOT NULL,
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uq_schema_version_migration` (`migration`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
                """;

        try (Statement statement = connection.createStatement()) {
            statement.execute(ddl);
        }
        connection.commit();
    }

    private Map<String, String> loadApplied(Connection connection) throws SQLException {
        Map<String, String> applied = new HashMap<>();

        try (PreparedStatement statement =
                     connection.prepareStatement("SELECT migration, checksum FROM schema_version");
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                applied.put(rs.getString("migration"), rs.getString("checksum"));
            }
        }
        return applied;
    }

    private void verifyNothingChanged(Map<String, String> applied, List<String> planned) {
        for (String name : planned) {
            String recorded = applied.get(name);
            if (recorded == null) {
                continue;
            }

            String current = checksum(read(name));
            if (!recorded.equals(current)) {
                throw new MigrationException(
                        "Migration " + name + " has changed since it was applied. "
                        + "A migration that has shipped must never be edited - "
                        + "add a new one instead.");
            }
        }
    }

    private void apply(Connection connection, String name) throws SQLException {
        String sql = read(name);
        List<String> statements = SqlScript.split(sql);

        log.info("Applying {} ({} statements)...", name, statements.size());
        Instant started = Instant.now();

        try (Statement statement = connection.createStatement()) {
            for (String each : statements) {
                try {
                    statement.execute(each);
                } catch (SQLException e) {
                    throw new MigrationException(
                            "Migration " + name + " failed on: " + summarise(each), e);
                }
            }
        }

        long millis = Duration.between(started, Instant.now()).toMillis();
        record(connection, name, checksum(sql), millis);
        connection.commit();

        log.info("Applied {} in {} ms.", name, millis);
    }

    private void record(Connection connection, String name, String checksum, long millis)
            throws SQLException {

        String sql = """
                INSERT INTO `schema_version` (`migration`, `checksum`, `applied_at`, `duration_ms`)
                VALUES (?, ?, NOW(), ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, checksum);
            statement.setInt(3, (int) millis);
            statement.executeUpdate();
        }
    }

    // ------------------------------------------------------------------

    private List<String> readIndex() {
        List<String> names = new ArrayList<>();

        try (InputStream in = MigrationRunner.class.getResourceAsStream(INDEX)) {
            if (in == null) {
                throw new MigrationException("Missing " + INDEX + " in the build.");
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8));

            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    names.add(trimmed);
                }
            }
        } catch (IOException e) {
            throw new MigrationException("Could not read " + INDEX, e);
        }

        return names;
    }

    private String read(String name) {
        try (InputStream in = MigrationRunner.class.getResourceAsStream(LOCATION + name)) {
            if (in == null) {
                throw new MigrationException(
                        "Migration " + name + " is listed in the index but not in the build.");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MigrationException("Could not read migration " + name, e);
        }
    }

    private String checksum(String content) {
        // Line endings differ between a Windows checkout and a Linux build, and
        // that must not read as a changed migration.
        String normalised = content.replace("\r\n", "\n");

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalised.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();

        } catch (Exception e) {
            throw new MigrationException("Could not checksum a migration.", e);
        }
    }

    private String summarise(String statement) {
        String oneLine = statement.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= 140 ? oneLine : oneLine.substring(0, 140) + "...";
    }
}
