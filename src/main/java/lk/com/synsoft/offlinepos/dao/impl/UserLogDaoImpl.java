package lk.com.synsoft.offlinepos.dao.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

import lk.com.synsoft.offlinepos.dao.UserLogDao;
import lk.com.synsoft.offlinepos.db.BaseDao;

public final class UserLogDaoImpl extends BaseDao implements UserLogDao {

    /** {@code logStat}: 1 while the session is open, 0 once it has ended. */
    private static final int OPEN = 1;
    private static final int CLOSED = 0;

    @Override
    public int closeOpenSessions(Connection connection, int userId, LocalDateTime at)
            throws SQLException {

        return update(connection, """
                UPDATE userlog
                   SET logStat = ?, logEnd = ?
                 WHERE user_USID = ?
                   AND logStat = ?
                """, CLOSED, at, userId, OPEN);
    }

    @Override
    public int openSession(Connection connection, int userId, LocalDateTime at) throws SQLException {
        // logEnd is set to the start time rather than left null, matching what
        // the legacy setUserLog wrote. An open session is the one with
        // logStat = 1, not the one with a null end.
        return insert(connection, """
                INSERT INTO userlog (logStart, logEnd, logStat, user_USID)
                VALUES (?, ?, ?, ?)
                """, at, at, OPEN, userId);
    }

    @Override
    public void closeSession(Connection connection, int logId, LocalDateTime at) throws SQLException {
        updateOne(connection, "Closing a session",
                "UPDATE userlog SET logStat = ?, logEnd = ? WHERE ULID = ?",
                CLOSED, at, logId);
    }
}
