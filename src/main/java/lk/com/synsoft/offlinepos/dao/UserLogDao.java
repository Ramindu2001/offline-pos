package lk.com.synsoft.offlinepos.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * The {@code userlog} table: who was signed in, and when.
 *
 * On a standalone till this is the only record of who was at the counter when
 * something went wrong, so it is written inside the login transaction rather
 * than afterwards - a session that opened without a log row would be invisible.
 */
public interface UserLogDao {

    /**
     * Closes any session left open for this user.
     *
     * A till that lost power never wrote an end time. Stamping the stale rows
     * closed at login keeps "who is signed in now" answerable, which the legacy
     * {@code editUserLogStat} did for the same reason.
     *
     * @return how many stale sessions were closed
     */
    int closeOpenSessions(Connection connection, int userId, LocalDateTime at) throws SQLException;

    /** Opens a session. Returns the {@code ULID} so logout can close this exact row. */
    int openSession(Connection connection, int userId, LocalDateTime at) throws SQLException;

    void closeSession(Connection connection, int logId, LocalDateTime at) throws SQLException;
}
