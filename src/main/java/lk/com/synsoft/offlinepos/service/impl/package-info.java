/**
 * Service implementations - business rules, and the transaction boundary.
 *
 * Rule: this is the ONLY layer that starts a transaction. An implementation
 * opens one unit of work, enforces permission and shop scope, calls whatever
 * DAOs it needs with that same Connection, then commits or rolls back.
 *
 * This is the fix for defect D01. In the legacy app {@code beginTransaction()}
 * was defined once and called from a single controller in 96,000 lines, so a
 * sale wrote six tables with no atomicity and any mid-way failure left half a
 * document behind.
 */
package lk.com.synsoft.offlinepos.service.impl;
