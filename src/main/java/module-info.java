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
    requires com.zaxxer.hikari;

    // An automatic module: the bcrypt jar declares no module-info and no
    // Automatic-Module-Name, so this name is derived from the file name. That is
    // also why Phase 16 cannot use jlink as the pom currently configures it -
    // jlink refuses to link automatic modules, and jpackage over a classpath
    // application is the way out.
    requires bcrypt;

    // JavaFX constructs the Application subclass reflectively.
    opens lk.com.synsoft.offlinepos.app to javafx.graphics, javafx.fxml;

    // FXML controllers are instantiated and their @FXML fields injected
    // reflectively, so the package has to be open to the loader.
    opens lk.com.synsoft.offlinepos.controller to javafx.fxml;

    exports lk.com.synsoft.offlinepos.app;
    exports lk.com.synsoft.offlinepos.config;
}
