package lk.com.synsoft.offlinepos.dto.auth;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * The shop the till is open in: its identity, its address as the receipt prints
 * it, and the 25 switches that decide which parts of the program exist here.
 *
 * @param flags only the switches that are on, so a flag added to the schema
 *              later reads as off on an older row rather than as unknown
 */
public record ShopProfile(
        int id,
        String shopNo,
        String name,
        String addressLineOne,
        String addressLineTwo,
        String city,
        String phone,
        String email,
        int companyId,
        boolean retail,
        boolean wholesale,
        boolean active,
        Set<ShopFlag> flags) {

    public ShopProfile {
        flags = flags == null || flags.isEmpty()
                ? Collections.unmodifiableSet(EnumSet.noneOf(ShopFlag.class))
                : Collections.unmodifiableSet(EnumSet.copyOf(flags));
    }

    /** Whether this shop has the area at all. See {@link ShopFlag}. */
    public boolean has(ShopFlag flag) {
        return flags.contains(flag);
    }

    /** The address block a receipt prints, with the parts that are missing left out. */
    public String addressBlock() {
        return java.util.stream.Stream.of(addressLineOne, addressLineTwo, city)
                .filter(part -> part != null && !part.isBlank())
                .map(String::trim)
                .collect(java.util.stream.Collectors.joining(", "));
    }
}
