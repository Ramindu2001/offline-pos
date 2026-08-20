package lk.com.synsoft.offlinepos.dto.auth;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Gate 1 of 3: is the company licence good today.
 *
 * <b>The expiry date is the last valid day.</b> A licence reading 31 December
 * works on 31 December and stops on 1 January. The legacy check was
 * {@code $today < $company['ComExpireDate']} on two strings, which locked the
 * shop out on the expiry date itself - a whole trading day early, with no way
 * for the shop to tell whether it was a licence problem or a bug (defect D12).
 *
 * Written against a supplied {@code today} rather than reading the clock
 * itself, so the boundary can be tested on both sides of midnight instead of
 * only on the day the test happens to run.
 *
 * @param daysLeft days remaining including today; 0 on the expiry date itself,
 *                 negative once it has passed
 */
public record LicenceStatus(
        boolean valid,
        boolean inactive,
        boolean expired,
        LocalDate expiryDate,
        long daysLeft) {

    /** Show the shop a warning from here on, as the reference app does. */
    public static final long WARN_WITHIN_DAYS = 30;

    public static LicenceStatus of(CompanyProfile company, LocalDate today) {
        boolean inactive = !company.active();

        LocalDate expiry = company.expiryDate();

        // No expiry date set is a perpetual licence, not an expired one. The
        // column is nullable and a fresh install can legitimately have no date.
        boolean expired = expiry != null && today.isAfter(expiry);
        long daysLeft = expiry == null ? Long.MAX_VALUE : ChronoUnit.DAYS.between(today, expiry);

        return new LicenceStatus(!inactive && !expired, inactive, expired, expiry, daysLeft);
    }

    /** True while the licence is still good but close enough to say so. */
    public boolean expiringSoon() {
        return valid && daysLeft <= WARN_WITHIN_DAYS;
    }
}
