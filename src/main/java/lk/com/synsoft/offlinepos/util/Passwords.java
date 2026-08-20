package lk.com.synsoft.offlinepos.util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;

/**
 * Password hashing, deliberately identical to what the PHP application already
 * writes.
 *
 * Cloud POS calls {@code password_hash($password, PASSWORD_DEFAULT)}, which is
 * bcrypt at cost 10 in the {@code $2y$} format. Producing exactly that here
 * means the two systems can read each other's hashes: an existing shop's
 * accounts work in this application on day one, without a password reset for
 * every cashier.
 *
 * Two details that matter and are easy to get wrong:
 *
 * <b>Passwords are char arrays, not Strings.</b> A String cannot be cleared and
 * sits in the heap until it is collected, so it can be read out of a crash dump
 * long after the login finished. Callers should clear the array when done;
 * {@link #verify} and {@link #hash} do not clear it for them, because the caller
 * may still need it.
 *
 * <b>bcrypt only reads the first 72 bytes.</b> PHP silently truncates there, so
 * verification does the same or an old long password would stop working. Setting
 * a new one refuses instead - quietly ignoring the end of what someone typed is
 * not something to carry forward.
 */
public final class Passwords {

    /** PHP's PASSWORD_DEFAULT cost. Changing this would orphan every existing hash. */
    private static final int COST = 10;

    /** bcrypt hashes at most this many bytes of a password. */
    public static final int MAX_BYTES = 72;

    private static final BCrypt.Version VERSION = BCrypt.Version.VERSION_2Y;

    private Passwords() {
    }

    /**
     * Hashes a new password.
     *
     * @throws IllegalArgumentException if it is empty, or longer than bcrypt can read
     */
    public static String hash(char[] raw) {
        if (raw == null || raw.length == 0) {
            throw new IllegalArgumentException("A password cannot be empty.");
        }
        if (byteLength(raw) > MAX_BYTES) {
            throw new IllegalArgumentException(
                    "A password can be at most " + MAX_BYTES + " bytes long.");
        }

        return BCrypt.with(VERSION, LongPasswordStrategies.truncate(VERSION))
                .hashToString(COST, raw);
    }

    /**
     * Checks a password against a stored hash.
     *
     * Returns false rather than throwing on a hash the library cannot read. A
     * corrupt or empty {@code UserPwd} column must fail the login like any wrong
     * password, not crash the login screen - and it must not tell the person at
     * the keyboard which of the two it was.
     */
    public static boolean verify(char[] raw, String storedHash) {
        if (raw == null || raw.length == 0 || storedHash == null || storedHash.isBlank()) {
            return false;
        }

        try {
            return BCrypt.verifyer(VERSION, LongPasswordStrategies.truncate(VERSION))
                    .verify(raw, storedHash)
                    .verified;

        } catch (IllegalArgumentException e) {
            // Thrown for a hash that is not bcrypt at all - a plaintext password
            // left in the column by hand, for instance.
            return false;
        }
    }

    /**
     * Whether a stored hash is one this application recognises.
     *
     * Used by the login flow to tell "wrong password" from "this account was
     * never set up properly", which is a different thing to log.
     */
    public static boolean isSupportedHash(String storedHash) {
        if (storedHash == null || storedHash.length() < 59) {
            return false;
        }
        return storedHash.startsWith("$2y$")
                || storedHash.startsWith("$2a$")
                || storedHash.startsWith("$2b$");
    }

    /** How many bytes bcrypt would actually see, which is not the character count. */
    public static int byteLength(char[] raw) {
        byte[] bytes = new String(raw).getBytes(StandardCharsets.UTF_8);
        int length = bytes.length;
        Arrays.fill(bytes, (byte) 0);
        return length;
    }
}
