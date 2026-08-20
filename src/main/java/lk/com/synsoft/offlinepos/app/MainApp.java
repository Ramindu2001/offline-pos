package lk.com.synsoft.offlinepos.app;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lk.com.synsoft.offlinepos.config.AppConfig;
import lk.com.synsoft.offlinepos.config.AppPaths;
import lk.com.synsoft.offlinepos.config.DataSourceProvider;
import lk.com.synsoft.offlinepos.db.StartupCheck;
import lk.com.synsoft.offlinepos.error.ErrorHandler;
import lk.com.synsoft.offlinepos.ui.BackgroundTasks;
import lk.com.synsoft.offlinepos.ui.Shortcuts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The JavaFX application.
 *
 * It does three things and then gets out of the way: check that the till can
 * work at all, wire the services, and hand the window to the router. Everything
 * after that is a route.
 *
 * When a check fails there is no router and no login screen - only a card
 * saying which check failed. Starting the application over a database it could
 * not reach would replace one clear sentence with a stream of failures further
 * from the cause.
 */
public class MainApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(MainApp.class);

    private Services services;
    private ViewRouter router;

    @Override
    public void start(Stage stage) {
        AppConfig config = AppConfig.get();

        log.info("Starting {} {}", config.appName(), config.appVersion());
        log.info("Settings: {}", config.describe());
        log.info("Data folder: {}", AppPaths.dataDir());

        // Anything thrown off a background task would otherwise vanish: a
        // packaged desktop app has no console for the default handler to print
        // to. This has to be in place before the first task is started.
        ErrorHandler.installUncaughtHandler();

        Scene scene = new Scene(new StackPane(), 1280, 800);
        scene.getStylesheets().add(
                MainApp.class.getResource("/lk/com/synsoft/offlinepos/css/app.css").toExternalForm());

        stage.setTitle(config.appName());
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(700);
        stage.setMaximized(true);

        startUp(stage, scene);

        stage.show();
    }

    private void startUp(Stage stage, Scene scene) {
        try {
            // The till has no server to migrate it and nobody to call, so it
            // checks itself at every launch, before anything can read or write
            // a row.
            StartupCheck.Report report = new StartupCheck(DataSourceProvider.get()).run();

            if (!report.ok()) {
                scene.setRoot(new StackPane(failedCard(report)));
                log.warn("Startup blocked; the login screen was not reached.");
                return;
            }

            services = new Services(DataSourceProvider.get());
            router = new ViewRouter(stage, scene, services);

            Shortcuts.install(scene, router);

            router.go(Route.LOGIN);
            log.info("Ready. Waiting for a sign-in.");

        } catch (RuntimeException e) {
            scene.setRoot(new StackPane(messageCard(
                    ErrorHandler.explain("Startup", e), "Logs: " + AppPaths.logDir())));
        }
    }

    // ------------------------------------------------------------------

    private VBox failedCard(StartupCheck.Report report) {
        VBox card = card();

        card.getChildren().addAll(
                heading("OfflinePOS cannot start", "h1"),
                muted("One of the checks below has to pass before the program can be used."),
                new Label());

        for (StartupCheck.Check check : report.checks()) {
            card.getChildren().add(checkLine(check));
        }

        card.getChildren().addAll(new Label(), mono("Logs: " + AppPaths.logDir()));
        return card;
    }

    private VBox messageCard(String message, String footnote) {
        VBox card = card();
        card.getChildren().addAll(
                heading("OfflinePOS cannot start", "h1"), wrapped(message), new Label(), mono(footnote));
        return card;
    }

    private HBox checkLine(StartupCheck.Check check) {
        Label name = new Label(check.name());
        name.setMinWidth(110);
        name.getStyleClass().add("mono");

        Label detail = wrapped(check.detail());
        detail.getStyleClass().add(switch (check.status()) {
            case PASS -> "status-ok";
            case FAIL -> "status-error";
            case SKIPPED -> "muted";
        });
        HBox.setHgrow(detail, Priority.ALWAYS);

        HBox line = new HBox(12, name, detail);
        line.setAlignment(Pos.TOP_LEFT);
        return line;
    }

    private VBox card() {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(760);
        card.setMaxHeight(VBox.USE_PREF_SIZE);
        StackPane.setMargin(card, new Insets(40));
        return card;
    }

    private Label heading(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    private Label muted(String text) {
        return heading(text, "muted");
    }

    private Label mono(String text) {
        Label label = new Label(text);
        label.getStyleClass().addAll("mono", "muted");
        return label;
    }

    /** Long sentences wrap instead of running off the card. */
    private Label wrapped(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(620);
        return label;
    }

    @Override
    public void stop() {
        // Phase 15 takes a backup here as well.
        log.info("Shutting down.");

        // Closing the window is how a shift usually ends, so the session is
        // closed here too. Without it every till would accumulate sessions that
        // look open forever, and "who was signed in" stops being answerable.
        if (services != null && Session.isSignedIn()) {
            services.auth().signOut(Session.current());
            Session.end();
        }

        BackgroundTasks.shutdown();
        DataSourceProvider.close();
    }
}
