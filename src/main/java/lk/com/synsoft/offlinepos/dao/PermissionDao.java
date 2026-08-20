package lk.com.synsoft.offlinepos.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import lk.com.synsoft.offlinepos.dto.auth.Rights;

/**
 * The {@code userroleaccess} rows for one role.
 */
public interface PermissionDao {

    /**
     * The whole rights matrix for a role, keyed by {@code sysfeatures.SFID}.
     *
     * Loaded once at login and held in the {@code AppContext}, not queried per
     * check: the legacy sidebar ran one lookup per menu item, 2,773 lines of
     * them (defect D15), and a permission check that costs a round trip is one
     * people start skipping.
     *
     * Features with no row are simply absent from the map, which reads as no
     * rights at all.
     */
    Map<Integer, Rights> findRightsForRole(Connection connection, int roleId) throws SQLException;

    /** Replaces every right for a role. Phase 6 builds the screen that calls this. */
    void replaceRightsForRole(Connection connection, int roleId, Map<Integer, Rights> rights)
            throws SQLException;
}
