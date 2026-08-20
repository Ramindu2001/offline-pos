package lk.com.synsoft.offlinepos.dto.auth;

import java.time.LocalDate;

/**
 * The company the shop belongs to, and the licence it runs under.
 *
 * {@code expiryDate} is a {@link LocalDate}, not a string. The legacy check
 * compared {@code date("Y-m-d")} against the column as text, which happens to
 * sort correctly and still locked the shop out a day early because it used
 * {@code <} rather than {@code <=} - defect D12. {@link LicenceStatus} is where
 * that is settled once.
 */
public record CompanyProfile(
        int id,
        String companyNo,
        String name,
        String location,
        String licenceNo,
        String versionNo,
        LocalDate startDate,
        LocalDate expiryDate,
        boolean active,
        boolean multiCategory,
        boolean commonStock) {
}
