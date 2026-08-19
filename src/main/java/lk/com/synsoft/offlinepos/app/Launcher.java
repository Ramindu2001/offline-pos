package lk.com.synsoft.offlinepos.app;

import javafx.application.Application;
import lk.com.synsoft.offlinepos.config.AppPaths;

/**
 * The process entry point.
 *
 * It is deliberately NOT the Application subclass. A main class that extends
 * Application refuses to start unless the JavaFX runtime is on the module path,
 * which makes packaging and IDE launches fragile. A plain class that calls
 * Application.launch() works either way, so this indirection stays.
 *
 * Its one job before handing over: decide where the log file goes, before
 * anything creates a logger. Once a logger exists the configuration is already
 * fixed, so this has to happen first.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        System.setProperty("offlinepos.logdir", AppPaths.logDir().toString());

        Application.launch(MainApp.class, args);
    }
}
