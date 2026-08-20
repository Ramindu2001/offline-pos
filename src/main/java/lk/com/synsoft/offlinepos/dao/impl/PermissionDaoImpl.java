package lk.com.synsoft.offlinepos.dao.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lk.com.synsoft.offlinepos.dao.PermissionDao;
import lk.com.synsoft.offlinepos.db.BaseDao;
import lk.com.synsoft.offlinepos.db.Rows;
import lk.com.synsoft.offlinepos.dto.auth.Rights;

public final class PermissionDaoImpl extends BaseDao implements PermissionDao {

    /**
     * Note the column name: {@code UserRolls_URID}, with the typo the live
     * schema has carried since the beginning. It is preserved rather than
     * corrected, because renaming it would break nothing here and everything in
     * a shop's existing data if this ever meets one.
     */
    private static final String SELECT_RIGHTS = """
            SELECT SysFeatures_SFID, is_view, is_create, is_edit, is_delete, is_verify, is_print
              FROM userroleaccess
             WHERE UserRolls_URID = ?
            """;

    /** One row of the matrix, so the mapper stays a pure function of the row. */
    private record FeatureRights(int featureId, Rights rights) {
    }

    @Override
    public Map<Integer, Rights> findRightsForRole(Connection connection, int roleId) throws SQLException {
        List<FeatureRights> rows = queryList(connection, SELECT_RIGHTS,
                row -> new FeatureRights(
                        Rows.integer(row, "SysFeatures_SFID"),
                        new Rights(
                                Rows.flag(row, "is_view"),
                                Rows.flag(row, "is_create"),
                                Rows.flag(row, "is_edit"),
                                Rows.flag(row, "is_delete"),
                                Rows.flag(row, "is_verify"),
                                Rows.flag(row, "is_print"))),
                roleId);

        Map<Integer, Rights> matrix = new HashMap<>(rows.size());
        for (FeatureRights row : rows) {
            matrix.put(row.featureId(), row.rights());
        }
        return matrix;
    }

    /**
     * Delete then insert, inside the caller's transaction.
     *
     * Rights are a set, not a list of edits: writing them any other way leaves
     * a role holding a right the screen no longer shows, which is how a
     * permissions page and the permissions it grants drift apart.
     */
    @Override
    public void replaceRightsForRole(Connection connection, int roleId, Map<Integer, Rights> rights)
            throws SQLException {

        update(connection, "DELETE FROM userroleaccess WHERE UserRolls_URID = ?", roleId);

        List<Object[]> rows = new ArrayList<>(rights.size());

        rights.forEach((featureId, right) -> rows.add(new Object[] {
                roleId, featureId,
                right.view(), right.create(), right.edit(),
                right.delete(), right.verify(), right.print()}));

        batch(connection, """
                INSERT INTO userroleaccess
                  (UserRolls_URID, SysFeatures_SFID, is_view, is_create, is_edit,
                   is_delete, is_verify, is_print)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, rows);
    }
}
