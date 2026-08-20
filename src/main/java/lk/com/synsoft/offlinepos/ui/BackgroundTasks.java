package lk.com.synsoft.offlinepos.ui;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import lk.com.synsoft.offlinepos.error.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Work that must not happen on the JavaFX thread.
 *
 * Every query goes through here. A database call on the UI thread freezes the
 * window for as long as it takes, and a till that stops repainting with a
 * customer at the counter looks broken whether it is or not.
 *
 * <b>Busy is shown, never enforced.</b> {@link #busy()} drives a thin bar in the
 * header and nothing else - no modal, no glass pane, no disabled input. A
 * cashier mid-sale must be able to keep typing while a report loads behind them,
 * which is the whole point of a pool rather than the single connection this
 * build replaced.
 *
 * <pre>{@code
 * BackgroundTasks.run(
 *         () -> productService.search(text),
 *         results -> table.setItems(results),
 *         "Searching products");
 * }</pre>
 */
public final class BackgroundTasks {

    private static final Logger log = LoggerFactory.getLogger(BackgroundTasks.class);

    private static final ReadOnlyBooleanWrapper BUSY = new ReadOnlyBooleanWrapper(false);
    private static final AtomicInteger RUNNING = new AtomicInteger();

    /**
     * Daemon threads: a task still running must never keep the application alive
     * after the window closes.
     */
    private static final ExecutorService POOL = Executors.newFixedThreadPool(3, namedDaemons());

    private BackgroundTasks() {
    }

    /** True while at least one task is running. Bind a progress indicator to it. */
    public static ReadOnlyBooleanProperty busy() {
        return BUSY.getReadOnlyProperty();
    }

    /**
     * Runs work off the UI thread and delivers the result back on it.
     *
     * A failure is reported through {@link ErrorHandler}, so the stack reaches
     * the log and the user gets one sentence - never a dialog full of SQL.
     *
     * @param what a short description for the log line, in the shape "Loading
     *             products", used to explain a failure
     */
    public static <T> void run(Callable<T> work, Consumer<T> onSuccess, String what) {
        run(work, onSuccess, failure -> {}, what);
    }

    /** As {@link #run(Callable, Consumer, String)}, with the failure handled too. */
    public static <T> void run(Callable<T> work, Consumer<T> onSuccess,
                               Consumer<String> onFailure, String what) {

        started();

        POOL.execute(() -> {
            try {
                T result = work.call();
                Platform.runLater(() -> {
                    try {
                        onSuccess.accept(result);
                    } finally {
                        finished();
                    }
                });

            } catch (Throwable failure) {
                String message = ErrorHandler.explain(what, failure);
                Platform.runLater(() -> {
                    try {
                        onFailure.accept(message);
                    } finally {
                        finished();
                    }
                });
            }
        });
    }

    /** Stops accepting work. Called as the application shuts down. */
    public static void shutdown() {
        POOL.shutdownNow();
    }

    private static void started() {
        if (RUNNING.incrementAndGet() == 1) {
            onFxThread(() -> BUSY.set(true));
        }
    }

    private static void finished() {
        if (RUNNING.decrementAndGet() == 0) {
            onFxThread(() -> BUSY.set(false));
        }
    }

    /**
     * The counter is touched from both threads, but the property may only be
     * written on the JavaFX one.
     */
    private static void onFxThread(Runnable work) {
        if (Platform.isFxApplicationThread()) {
            work.run();
        } else {
            Platform.runLater(work);
        }
    }

    private static ThreadFactory namedDaemons() {
        AtomicInteger counter = new AtomicInteger();

        return runnable -> {
            Thread thread = new Thread(runnable, "offlinepos-task-" + counter.incrementAndGet());
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler(
                    (t, error) -> log.error("Task thread {} died.", t.getName(), error));
            return thread;
        };
    }
}
