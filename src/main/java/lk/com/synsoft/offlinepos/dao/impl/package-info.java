/**
 * DAO implementations.
 *
 * Rule: explicit SQL per method. No generic "insert anything" helper that
 * builds a table name or column list from caller-supplied strings - that was
 * defect D14.
 */
package lk.com.synsoft.offlinepos.dao.impl;
