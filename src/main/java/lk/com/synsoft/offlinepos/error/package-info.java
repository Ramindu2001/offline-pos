/**
 * The application's exception vocabulary, and the one place a failure is turned
 * into something a cashier can read.
 *
 * This package sits below every layer and depends on nothing, so any layer may
 * throw from it without inverting the dependency direction.
 *
 * It replaces the legacy pattern of {@code die("Error: " . $e->getMessage())}
 * scattered through the model layer with display_errors on (defect D09), which
 * showed the user raw SQL, ended the request where it stood, and rolled nothing
 * back.
 *
 * Two kinds of failure, deliberately split:
 *
 *   Checked ({@link lk.com.synsoft.offlinepos.error.AppException}) - the caller
 *   is expected to do something about it. A service declares these so the
 *   controller has to decide what the user sees.
 *
 *   Unchecked ({@link lk.com.synsoft.offlinepos.error.DataAccessException}) -
 *   infrastructure gave way. No caller can meaningfully recover, so it is not
 *   forced into every signature between here and the screen.
 *
 * Both carry a user-facing sentence through
 * {@link lk.com.synsoft.offlinepos.error.UserFacing}.
 */
package lk.com.synsoft.offlinepos.error;
