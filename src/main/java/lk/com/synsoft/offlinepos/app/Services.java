package lk.com.synsoft.offlinepos.app;

import java.time.Clock;

import javax.sql.DataSource;

import lk.com.synsoft.offlinepos.dao.PermissionDao;
import lk.com.synsoft.offlinepos.dao.ShopDao;
import lk.com.synsoft.offlinepos.dao.UserDao;
import lk.com.synsoft.offlinepos.dao.UserLogDao;
import lk.com.synsoft.offlinepos.dao.impl.PermissionDaoImpl;
import lk.com.synsoft.offlinepos.dao.impl.ShopDaoImpl;
import lk.com.synsoft.offlinepos.dao.impl.UserDaoImpl;
import lk.com.synsoft.offlinepos.dao.impl.UserLogDaoImpl;
import lk.com.synsoft.offlinepos.db.TransactionManager;
import lk.com.synsoft.offlinepos.service.AuthService;
import lk.com.synsoft.offlinepos.service.PermissionService;
import lk.com.synsoft.offlinepos.service.impl.AuthServiceImpl;
import lk.com.synsoft.offlinepos.service.impl.PermissionServiceImpl;

/**
 * The composition root: the one place that knows which implementation is behind
 * each interface.
 *
 * Constructor wiring by hand rather than a container. There are perhaps forty
 * services to come and the graph is a straight line, so a framework would add a
 * dependency, a startup cost and a class of runtime failure without removing a
 * single decision from this file.
 *
 * The only piece of indirection that earns its place is {@code Session::current}
 * being handed to {@link PermissionService} as a supplier. That is what lets a
 * service ask who is signed in without the service layer importing this one, and
 * what makes switching shop a matter of replacing one immutable context.
 *
 * Phase 4 adds the router and the controllers on top; every service they need is
 * reached from here.
 */
public final class Services {

    private final TransactionManager transactions;

    private final UserDao userDao;
    private final ShopDao shopDao;
    private final PermissionDao permissionDao;
    private final UserLogDao userLogDao;

    private final PermissionService permissions;
    private final AuthService auth;

    public Services(DataSource dataSource, Clock clock) {
        this.transactions = new TransactionManager(dataSource);

        this.userDao = new UserDaoImpl();
        this.shopDao = new ShopDaoImpl();
        this.permissionDao = new PermissionDaoImpl();
        this.userLogDao = new UserLogDaoImpl();

        this.permissions = new PermissionServiceImpl(Session::current);

        this.auth = new AuthServiceImpl(
                transactions, userDao, shopDao, permissionDao, userLogDao, permissions, clock);
    }

    public Services(DataSource dataSource) {
        this(dataSource, Clock.systemDefaultZone());
    }

    public AuthService auth() {
        return auth;
    }

    public PermissionService permissions() {
        return permissions;
    }

    /** For the services Phase 6 onwards will be constructed with. */
    public TransactionManager transactions() {
        return transactions;
    }
}
