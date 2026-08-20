/**
 * The security vocabulary: who is signed in, which shop the till is open in,
 * and what may be done there.
 *
 * Three separate questions, and every screen is gated by all three at once. The
 * legacy system asked them too, in 2,773 lines of copy-pasted sidebar checks
 * (defect D15), so the point of this package is that each is asked in exactly
 * one place.
 *
 * <ul>
 *   <li>{@link lk.com.synsoft.offlinepos.dto.auth.LicenceStatus} - is the
 *       company licence good today. Gate 1, and the home of the off-by-one that
 *       was defect D12.</li>
 *   <li>{@link lk.com.synsoft.offlinepos.dto.auth.ShopFlag} - does this shop
 *       have the area at all. Gate 2, and a question about the shop rather than
 *       the person.</li>
 *   <li>{@link lk.com.synsoft.offlinepos.dto.auth.Rights} per
 *       {@link lk.com.synsoft.offlinepos.dto.auth.Feature} - may this role do
 *       it. Gate 3, short-circuited for a
 *       {@link lk.com.synsoft.offlinepos.dto.auth.UserType#SUPER_ADMIN}.</li>
 * </ul>
 *
 * {@link lk.com.synsoft.offlinepos.dto.auth.AppContext} holds the answers for
 * one session and is the only place a shop id may come from.
 */
package lk.com.synsoft.offlinepos.dto.auth;
