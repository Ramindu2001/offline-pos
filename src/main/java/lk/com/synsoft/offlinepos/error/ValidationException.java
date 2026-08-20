package lk.com.synsoft.offlinepos.error;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The request cannot be carried out because of what it contains.
 *
 * Thrown by the service, never by the controller. A controller may also check a
 * field before it calls - that is a courtesy to the user, not the enforcement.
 * The rule holds at the service or it does not hold, because Phase 8's till and
 * Phase 14's reports call the same methods from different screens.
 */
public class ValidationException extends AppException {

    private final List<String> problems;

    public ValidationException(String problem) {
        this(List.of(problem));
    }

    public ValidationException(List<String> problems) {
        super(join(problems), "Validation failed: " + join(problems));
        this.problems = List.copyOf(problems);
    }

    /** Every problem found, so the screen can mark more than one field at once. */
    public List<String> problems() {
        return problems;
    }

    private static String join(List<String> problems) {
        if (problems.isEmpty()) {
            return "The details entered are not valid.";
        }
        return String.join(" ", problems);
    }

    /**
     * Collects problems and throws only if any were found.
     *
     * The legacy screens stopped at the first bad field, so a cashier fixed one
     * thing, resubmitted, and was told about the next. This reports them together.
     */
    public static final class Check {

        private final List<String> problems = new ArrayList<>();

        public Check require(boolean condition, String problem) {
            if (!condition) {
                problems.add(problem);
            }
            return this;
        }

        public Check reject(boolean condition, String problem) {
            return require(!condition, problem);
        }

        public List<String> problems() {
            return Collections.unmodifiableList(problems);
        }

        public void throwIfFailed() throws ValidationException {
            if (!problems.isEmpty()) {
                throw new ValidationException(problems);
            }
        }
    }
}
