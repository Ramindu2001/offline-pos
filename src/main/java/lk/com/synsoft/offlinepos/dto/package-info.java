/**
 * Data carriers crossing the controller/service boundary, one sub-package per
 * domain, mirroring the nine ER modules.
 *
 * Rule: no JavaFX types in here. Money is {@link java.math.BigDecimal} and
 * dates are {@link java.time.LocalDate} or {@link java.time.LocalDateTime} -
 * never double, never a String date. Both of those cost real money in a POS:
 * double loses cents on a long bill, and string dates are how the legacy
 * licence check ended up off by one (defect D12).
 */
package lk.com.synsoft.offlinepos.dto;
