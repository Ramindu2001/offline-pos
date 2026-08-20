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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The JavaFX application.
 *
 * Phase 2 proves the plumbing underneath: the self-check runs, the schema is
 * brought up to date, and a failure at any of it produces one readable sentence
 * on screen instead of a stack trace on a console nobody is looking at.
 *
 * Phase 4 replaces the placeholder scene with the real shell (sidebar, header,
 * and the four layouts) and the router that fills it.
 */
public class MainApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(MainApp.class);

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

        stage.setTitle(config.appName());
        stage.setScene(buildScene(config));
        stage.setMinWidth(1024);
        stage.setMinHeight(700);
        stage.setMaximized(true);
        stage.show();
    }

    private Scene buildScene(AppConfig config) {
        VBox card;
        try {
            // The till has no server to migrate it and nobody to call, so it
            // checks itself at every launch, before anything can read or write
            // a row.
            StartupCheck.Report report = new StartupCheck(DataSourceProvider.get()).run();

            card = report.ok() ? readyCard(config, report) : failedCard(report);
            log.info("Window shown. Startup {}.", report.ok() ? "clean" : "blocked");

        } catch (RuntimeException e) {
            card = messageCard("OfflinePOS cannot start",
                    ErrorHandler.explain("Startup", e),
                    "Logs: " + AppPaths.logDir());
        }

        StackPane root = new StackPane(card);
        root.setPadding(new Insets(40));

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(
                MainApp.class.getResource("/lk/com/synsoft/offlinepos/css/app.css").toExternalForm());

        return scene;
    }

    // ------------------------------------------------------------------

    private VBox readyCard(AppConfig config, StartupCheck.Report report) {
        VBox card = card();

        card.getChildren().addAll(
                heading(config.appName(), "h1"),
                muted("Phase 2 - data access plumbing"),
                new Label(),
                mono(config.describe()),
                mono("Logs: " + AppPaths.logDir()),
                new Label());

        for (StartupCheck.Check check : report.checks()) {
            card.getChildren().add(checkLine(check));
        }

        card.getChildren().addAll(new Label(), muted("Next: Phase 3 - security, session and permissions"));
        return card;
    }

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

    private VBox messageCard(String title, String message, String footnote) {
        VBox card = card();
        card.getChildren().addAll(heading(title, "h1"), wrapped(message), new Label(), mono(footnote));
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

    // ------------------------------------------------------------------

    private VBox card() {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(760);
        card.setMaxHeight(VBox.USE_PREF_SIZE);
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
        DataSourceProvider.close();
    }
}
