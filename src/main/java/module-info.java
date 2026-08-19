/**
 * OfflinePOS.
 *
 * Layer rule, enforced by what each package is allowed to see:
 *   fxml -> controller -> dto -> service -> service.impl -> dao -> db
 *
 * The MySQL driver is deliberately NOT required here. JDBC drivers are found
 * through java.sql's ServiceLoader, so there is no need to name the connector
 * (whose automatic module name is derived from its filename and would break on
 * a version bump). It also means no Class.forName call is needed anywhere -
 * that has been unnecessary since JDBC 4.0.
 */
module lk.com.synsoft.offlinepos {

    requires javafx.controls;
    requires javafx.fxml;

    requires java.sql;
    requires org.slf4j;

    // JavaFX constructs the Application subclass reflectively.
    opens lk.com.synsoft.offlinepos.app to javafx.graphics, javafx.fxml;

    // Phase 4 adds:
    //   opens lk.com.synsoft.offlinepos.controller to javafx.fxml;
    // FXML controllers are instantiated and injected reflectively, so the
    // package must be open. It is left out until the first controller exists,
    // because opening an empty package is a compile warning.

    exports lk.com.synsoft.offlinepos.app;
    exports lk.com.synsoft.offlinepos.config;
}
