package lk.com.synsoft.offlinepos.config;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The connection pool.
 *
 * Replaces the scaffold's single static Connection, which could not survive a
 * MySQL wait_timeout and could not run a report while a sale was open. It is
 * also what makes the transaction boundary in Phase 2 possible: a unit of work
 * borrows one connection from here for its whole life and hands it back.
 */
public final class DataSourceProvider {

    private static final Logger log = LoggerFactory.getLogger(DataSourceProvider.class);

    private static HikariDataSource dataSource;

    private DataSourceProvider() {
    }

    public static synchronized DataSource get() {
        if (dataSource == null) {
            dataSource = build(AppConfig.get());
        }
        return dataSource;
    }

    private static HikariDataSource build(AppConfig config) {
        HikariConfig hikari = new HikariConfig();

        hikari.setJdbcUrl(config.jdbcUrl());
        hikari.setUsername(config.dbUser());
        hikari.setPassword(config.dbPassword());

        hikari.setMaximumPoolSize(config.dbPoolSize());
        hikari.setPoolName("offlinepos");

        // A till that cannot reach its database must say so quickly rather than
        // freezing with a customer waiting at the counter.
        hikari.setConnectionTimeout(5_000);
        hikari.setValidationTimeout(3_000);

        // Do not throw while building the pool when the server is down. The
        // startup self-check is what reports that, on screen and in one
        // sentence; a pool that refuses to be constructed would instead kill the
        // launch before there is a window to show it in.
        hikari.setInitializationFailTimeout(-1);

        // Every unit of work manages its own boundary, so a borrowed connection
        // must never quietly commit a statement on its own.
        hikari.setAutoCommit(false);

        log.info("Opening connection pool: {}", config.describe());

        return new HikariDataSource(hikari);
    }

    /** Closed from MainApp.stop(). */
    public static synchronized void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            log.info("Closing connection pool.");
            dataSource.close();
        }
        dataSource = null;
    }
}
