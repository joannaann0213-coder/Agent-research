package dev.agentflow.inbox.server.support;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Counts the SQL statements the application sends, so a test can assert "the happy path is exactly
 * one statement". Counting is per JVM; tests reset it before the action they measure.
 */
public final class StatementCounter {

    private static final AtomicInteger COUNT = new AtomicInteger();

    private StatementCounter() {
    }

    public static void reset() {
        COUNT.set(0);
    }

    public static int count() {
        return COUNT.get();
    }

    public static DataSource wrap(DataSource target) {
        return new DelegatingDataSource(target) {
            @Override
            public Connection getConnection() throws SQLException {
                return counting(super.getConnection());
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                return counting(super.getConnection(username, password));
            }
        };
    }

    private static Connection counting(Connection target) {
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            try {
                if (name.equals("prepareStatement") || name.equals("prepareCall")) {
                    COUNT.incrementAndGet();
                    return method.invoke(target, args);
                }
                if (name.equals("createStatement")) {
                    return countingStatement((Statement) method.invoke(target, args));
                }
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        };
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] {Connection.class}, handler);
    }

    private static Statement countingStatement(Statement target) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getName().startsWith("execute") && args != null && args.length > 0) {
                COUNT.incrementAndGet();
            }
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        };
        return (Statement) Proxy.newProxyInstance(Statement.class.getClassLoader(), new Class<?>[] {Statement.class}, handler);
    }
}
