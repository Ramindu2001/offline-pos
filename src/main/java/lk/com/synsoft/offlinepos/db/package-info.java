/**
 * Database plumbing: the transaction boundary, the DAO base class and its row
 * mappers, the schema migration runner, and the startup self-check.
 *
 * Nothing here is user-visible and all of it is load-bearing. The transaction
 * boundary in {@link lk.com.synsoft.offlinepos.db.TransactionManager} is what
 * separates this build from the one it replaces: it is the difference between a
 * failed sale leaving nothing behind and leaving half a document behind
 * (defect D01).
 *
 * The connection pool itself lives in
 * {@link lk.com.synsoft.offlinepos.config.DataSourceProvider}, because deciding
 * which server to open and with what credentials is configuration, not data
 * access.
 */
package lk.com.synsoft.offlinepos.db;
