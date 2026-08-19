/**
 * Database plumbing: the connection pool, the transaction manager, the DAO
 * base class and row mappers, and the schema migration runner.
 *
 * Built in Phase 2. Nothing here is user-visible and all of it is load-bearing:
 * the transaction boundary this package provides is what separates this build
 * from the one it replaces.
 */
package lk.com.synsoft.offlinepos.db;
