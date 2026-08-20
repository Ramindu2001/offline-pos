package lk.com.synsoft.offlinepos.error;

import java.util.Optional;

/**
 * The row a request names is not there.
 *
 * Separate from {@link ValidationException} because it usually means something
 * changed underneath the user - a product deactivated on another till, an
 * invoice already cancelled - not that they typed something wrong.
 */
public class NotFoundException extends AppException {

    private final String entity;

    public NotFoundException(String entity, Object id) {
        super("That " + entity.toLowerCase() + " no longer exists.",
              entity + " " + id + " was not found.");
        this.entity = entity;
    }

    public String entity() {
        return entity;
    }

    /**
     * Unwraps a lookup, turning the empty case into a failure.
     *
     * <pre>{@code
     * Product p = NotFoundException.require(dao.find(connection, id), "Product", id);
     * }</pre>
     */
    public static <T> T require(Optional<T> found, String entity, Object id)
            throws NotFoundException {

        return found.orElseThrow(() -> new NotFoundException(entity, id));
    }
}
