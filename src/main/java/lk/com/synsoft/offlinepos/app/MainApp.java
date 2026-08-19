package lk.com.synsoft.offlinepos.app;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lk.com.synsoft.offlinepos.config.AppConfig;
import lk.com.synsoft.offlinepos.config.AppPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The JavaFX application.
 *
 * Phase 0 only proves the foundation is sound: settings load, logging writes,
 * the stylesheet applies. Phase 4 replaces the placeholder scene with the real
 * shell (sidebar, header, and the four layouts), and the router that fills it.
 */
public class MainApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(MainApp.class);

    @Override
    public void start(Stage stage) {
        AppConfig config = AppConfig.get();

        log.info("Starting {} {}", config.appName(), config.appVersion());
        log.info("Settings: {}", config.describe());
        log.info("Data folder: {}", AppPaths.dataDir());

        // Anything that escapes a background task would otherwise vanish silently.
        Thread.setDefaultUncaughtExceptionHandler(
                (thread, error) -> log.error("Uncaught error on thread {}", thread.getName(), error));

        stage.setTitle(config.appName());
        stage.setScene(buildScene(config));
        stage.setMinWidth(1024);
        stage.setMinHeight(700);
        stage.setMaximized(true);
        stage.show();

        log.info("Window shown. Foundation is up.");
    }

    private Scene buildScene(AppConfig config) {
        Label title = new Label(config.appName());
        title.getStyleClass().add("h1");

        Label subtitle = new Label("Phase 0 - project foundation");
        subtitle.getStyleClass().add("muted");

        Label settings = new Label(config.describe());
        settings.getStyleClass().addAll("mono", "muted");

        Label logs = new Label("Logs: " + AppPaths.logDir());
        logs.getStyleClass().addAll("mono", "muted");

        Label next = new Label("Next: Phase 1 - database schema");
        next.getStyleClass().add("status-ok");

        VBox card = new VBox(10, title, subtitle, new Label(), settings, logs, new Label(), next);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(720);
        card.setMaxHeight(VBox.USE_PREF_SIZE);

        StackPane root = new StackPane(card);
        root.setPadding(new Insets(40));

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(
                MainApp.class.getResource("/lk/com/synsoft/offlinepos/css/app.css").toExternalForm());

        return scene;
    }

    @Override
    public void stop() {
        // Phase 2 closes the connection pool here; Phase 15 takes a backup.
        log.info("Shutting down.");
    }
}
