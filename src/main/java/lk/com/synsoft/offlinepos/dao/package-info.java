/**
 * DAO interfaces - SQL only, one per table group.
 *
 * Rule: every method takes {@link java.sql.Connection} as its first parameter.
 * That is what lets a service implementation run several DAOs inside one
 * transaction, and it is why this layer exists at all - the original design
 * had the service implementation talking to the database directly, which
 * cannot express a multi-table commit.
 *
 * Rule: {@link java.sql.PreparedStatement} with {@code ?} placeholders only.
 * String concatenation into SQL is banned outright. The legacy app called
 * prepare() 479 times but bound parameters in only 333 of them, splicing
 * variables straight into the statement in the rest - which looks safe and is
 * not (defect D08).
 */
package lk.com.synsoft.offlinepos.dao;
