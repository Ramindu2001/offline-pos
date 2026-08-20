package lk.com.synsoft.offlinepos.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compatibility with the hashes Cloud POS already writes.
 *
 * If this file goes red, an existing shop's accounts stop working the day they
 * move across - so the first test is the one that matters: the exact hash the
 * V4 bootstrap migration ships, verified against the password it was made from.
 */
class PasswordsTest {

    /** The hash in V4__bootstrap.sql, produced by PHP's password_hash(). */
    private static final String PHP_HASH =
            "$2y$10$N2EQvVR0j5gxkRSxM5AyVeOJppYs/cICIZTKWz2npGxf6RdNrlyiK";

    @Test
    @DisplayName("a hash written by PHP password_hash() verifies here")
    void verifiesPhpHash() {
        assertTrue(Passwords.verify("admin123".toCharArray(), PHP_HASH),
                "The bootstrap administrator can no longer sign in.");

        assertFalse(Passwords.verify("admin124".toCharArray(), PHP_HASH));
        assertFalse(Passwords.verify("".toCharArray(), PHP_HASH));
    }

    @Test
    @DisplayName("what we write is the same $2y$ format PHP produces")
    void writesPhpCompatibleFormat() {
        String hash = Passwords.hash("cashier-1".toCharArray());

        assertTrue(hash.startsWith("$2y$10$"), hash);
        assertEquals(60, hash.length(), hash);
        assertTrue(Passwords.verify("cashier-1".toCharArray(), hash));
    }

    @Test
    @DisplayName("the same password hashes differently every time, and both verify")
    void saltIsRandom() {
        String first = Passwords.hash("same-password".toCharArray());
        String second = Passwords.hash("same-password".toCharArray());

        assertFalse(first.equals(second), "Two hashes of one password must not match.");
        assertTrue(Passwords.verify("same-password".toCharArray(), first));
        assertTrue(Passwords.verify("same-password".toCharArray(), second));
    }

    @Test
    @DisplayName("older $2a$ hashes are still read")
    void readsOlderVersions() {
        // "abc" at cost 10 in the $2a$ format, as produced by older PHP builds.
        String legacy = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

        assertTrue(Passwords.isSupportedHash(legacy));
        assertFalse(Passwords.verify("wrong".toCharArray(), legacy));
    }

    @Test
    @DisplayName("a column holding something that is not a hash fails like a wrong password")
    void unreadableHashIsJustAFailedLogin() {
        assertFalse(Passwords.verify("anything".toCharArray(), "plaintext-password"));
        assertFalse(Passwords.verify("anything".toCharArray(), ""));
        assertFalse(Passwords.verify("anything".toCharArray(), null));

        assertFalse(Passwords.isSupportedHash("plaintext-password"));
        assertFalse(Passwords.isSupportedHash(null));
    }

    @Test
    @DisplayName("setting a password longer than bcrypt reads is refused, not truncated")
    void refusesOverlongNewPassword() {
        char[] tooLong = "x".repeat(Passwords.MAX_BYTES + 1).toCharArray();

        assertThrows(IllegalArgumentException.class, () -> Passwords.hash(tooLong));
        assertThrows(IllegalArgumentException.class, () -> Passwords.hash(new char[0]));
    }

    @Test
    @DisplayName("length is counted in bytes, because that is what bcrypt reads")
    void countsBytesNotCharacters() {
        assertEquals(3, Passwords.byteLength("abc".toCharArray()));

        // Sinhala, three bytes per character in UTF-8. A shop in Sri Lanka may
        // well use it, and 30 characters would be 90 bytes.
        assertEquals(9, Passwords.byteLength("ලංක".toCharArray()));
    }
}
