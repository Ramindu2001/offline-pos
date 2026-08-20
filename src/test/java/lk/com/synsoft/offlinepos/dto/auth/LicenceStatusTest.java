package lk.com.synsoft.offlinepos.dto.auth;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Defect D12, settled.
 *
 * The legacy check was {@code $today < $company['ComExpireDate']}, comparing two
 * strings, so a licence reading 31 December stopped working on 31 December - a
 * whole trading day early, with the shop told only that its licence had expired.
 *
 * The rule here is that the expiry date is the last valid day, and these are the
 * three days either side of it.
 */
class LicenceStatusTest {

    private static final LocalDate EXPIRY = LocalDate.of(2026, 12, 31);

    private static CompanyProfile company(boolean active, LocalDate expiry) {
        return new CompanyProfile(1, "CM_000001", "My Company", "Sri Lanka", "OFFLINE-0001", "1.0",
                LocalDate.of(2026, 1, 1), expiry, active, false, false);
    }

    @Test
    @DisplayName("the day before expiry is valid")
    void dayBefore() {
        LicenceStatus status = LicenceStatus.of(company(true, EXPIRY), EXPIRY.minusDays(1));

        assertTrue(status.valid());
        assertFalse(status.expired());
        assertEquals(1, status.daysLeft());
    }

    @Test
    @DisplayName("the expiry date itself is still valid - this is the whole of D12")
    void expiryDayItself() {
        LicenceStatus status = LicenceStatus.of(company(true, EXPIRY), EXPIRY);

        assertTrue(status.valid(), "The shop must still trade on its expiry date.");
        assertFalse(status.expired());
        assertEquals(0, status.daysLeft());
    }

    @Test
    @DisplayName("the day after expiry is not")
    void dayAfter() {
        LicenceStatus status = LicenceStatus.of(company(true, EXPIRY), EXPIRY.plusDays(1));

        assertFalse(status.valid());
        assertTrue(status.expired());
        assertEquals(-1, status.daysLeft());
    }

    @Test
    @DisplayName("a switched-off company is refused whatever the date says")
    void inactiveCompany() {
        LicenceStatus status = LicenceStatus.of(company(false, EXPIRY), EXPIRY.minusDays(100));

        assertFalse(status.valid());
        assertTrue(status.inactive());
        assertFalse(status.expired());
    }

    @Test
    @DisplayName("no expiry date is a perpetual licence, not an expired one")
    void noExpiryDate() {
        LicenceStatus status = LicenceStatus.of(company(true, null), LocalDate.of(2099, 1, 1));

        assertTrue(status.valid());
        assertFalse(status.expired());
    }

    @Test
    @DisplayName("the warning starts thirty days out and not before")
    void warnsWhenClose() {
        CompanyProfile company = company(true, EXPIRY);

        assertFalse(LicenceStatus.of(company, EXPIRY.minusDays(31)).expiringSoon());
        assertTrue(LicenceStatus.of(company, EXPIRY.minusDays(30)).expiringSoon());
        assertTrue(LicenceStatus.of(company, EXPIRY).expiringSoon());

        // Already expired is not "expiring soon" - it is a different screen.
        assertFalse(LicenceStatus.of(company, EXPIRY.plusDays(1)).expiringSoon());
    }
}
