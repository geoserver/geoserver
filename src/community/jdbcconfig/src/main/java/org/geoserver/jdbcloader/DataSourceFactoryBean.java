/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcloader;

import com.google.common.base.Optional;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.commons.dbcp.BasicDataSource;
import org.geotools.util.Converters;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

public class DataSourceFactoryBean implements FactoryBean<DataSource>, DisposableBean {

    private static final Logger LOGGER = Logging.getLogger(DataSourceFactoryBean.class);

    JDBCLoaderProperties config;
    Context jndiCtx;
    DataSource dataSource;

    private static Context getJNDI(JDBCLoaderProperties config) {
        if (config.isEnabled() && config.getJndiName().isPresent()) {
            try {
                return GeoTools.getInitialContext(GeoTools.getDefaultHints());
            } catch (NamingException ex) {
                LOGGER.log(
                        Level.WARNING,
                        "Could not get JNDI Context, will not use JNDI to locate DataSource",
                        ex);
                return null;
            }
        } else {
            // Don't bother trying to get a JNDI context if JNDI lookup isn't needed.
            return null;
        }
    }

    public DataSourceFactoryBean(JDBCLoaderProperties config) {
        this(config, getJNDI(config));
    }

    public DataSourceFactoryBean(JDBCLoaderProperties config, Context jndiCtx) {
        this.config = config;
        this.jndiCtx = jndiCtx;
    }

    @Override
    public DataSource getObject() throws Exception {

        if (dataSource == null) {
            if (!config.isEnabled()) {
                // hack, create a stub database so that other beans in the context like the
                // transaction manager can function, despite the fact that the plugin is
                // disabled
                dataSource = createDataSourceStub();
            } else {
                dataSource = lookupOrCreateDataSource();
            }
        }
        return dataSource;
    }

    protected DataSource createDataSourceStub() {
        return (DataSource)
                Proxy.newProxyInstance(
                        getClass().getClassLoader(),
                        new Class[] {DataSource.class},
                        new InvocationHandler() {
                            @Override
                            public Object invoke(Object proxy, Method method, Object[] args)
                                    throws Throwable {
                                return null;
                            }
                        });
    }

    /** Look up or create a DataSource */
    protected DataSource lookupOrCreateDataSource() throws Exception {
        DataSource ds = null;

        boolean jndi = true;
        ds = getJNDIDataSource(config.getJndiName()).orNull();

        if (ds == null) {
            jndi = false;
            ds = createDataSource();
        }

        // Open and close a connection to verify the datasource works.
        try {
            ds.getConnection().close();
        } catch (Exception ex) {
            // Provide a useful error message that won't get lost in the stack trace.
            if (jndi) {
                LOGGER.severe(
                        "Error connecting to JDBC database. Verify the settings of your JNDI data source and that the database is available.");
            } else {
                LOGGER.severe(
                        "Error connecting to JDBC database. Verify the settings in your properties file and that the database is available.");
            }
            throw ex;
        }
        return ds;
    }

    /** Get an unconfigured BasicDataSource to set up */
    protected BasicDataSource createBasicDataSource() {
        return new BasicDataSource();
    }

    /** Try to lookup a configured DataSource using JNDI. */
    protected Optional<DataSource> getJNDIDataSource(Optional<String> name) {
        if (jndiCtx == null) return Optional.absent();

        if (name.isPresent()) {
            try {
                Optional<DataSource> ds = Optional.of((DataSource) jndiCtx.lookup(name.get()));
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.log(Level.INFO, "JDBCLoader using JNDI DataSource {0}", name.get());
                }
                config.setDatasourceId(name.get());
                return ds;
            } catch (NamingException ex) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(
                            Level.WARNING,
                            "Could not resolve JNDI name "
                                    + name.get()
                                    + " for JDBCLoader Database",
                            ex);
                }
                return Optional.absent();
            }
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("No JNDI name given for JDBCLoader DB.");
            }
            return Optional.absent();
        }
    }

    /** Create and configure a DataSource based on the JDBCLoaderProperties */
    protected DataSource createDataSource() throws Exception {
        BasicDataSource dataSource = createBasicDataSource();

        dataSource.setUrl(config.getJdbcUrl().get());

        Optional<String> driverClassName = get(config, "driverClassName", String.class, true);
        try {
            Class.forName(driverClassName.get());
        } catch (Exception e) {
            throw new RuntimeException("Error loading jdbc driver class: " + driverClassName, e);
        }

        dataSource.setDriverClassName(driverClassName.get());
        dataSource.setUsername(get(config, "username", String.class, false).orNull());
        dataSource.setPassword(get(config, "password", String.class, false).orNull());

        dataSource.setMinIdle(get(config, "pool.minIdle", Integer.class, false).or(1));
        dataSource.setMaxActive(get(config, "pool.maxActive", Integer.class, false).or(10));
        dataSource.setPoolPreparedStatements(
                get(config, "pool.poolPreparedStatements", Boolean.class, false).or(true));
        dataSource.setMaxOpenPreparedStatements(
                get(config, "pool.maxOpenPreparedStatements", Integer.class, false).or(50));

        boolean testOnBorrow = get(config, "pool.testOnBorrow", Boolean.class, false).or(false);
        if (testOnBorrow) {
            String validateQuery = get(config, "pool.validationQuery", String.class, true).get();
            dataSource.setTestOnBorrow(true);
            dataSource.setValidationQuery(validateQuery);
        }

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.log(Level.INFO, "JDBCConfig using JDBC DataSource {0}", config.getJdbcUrl());
        }

        // TODO: more connection parameters
        config.setDatasourceId(config.getJdbcUrl().get());
        return dataSource;
    }

    <T> Optional<T> get(Properties props, String key, Class<T> clazz, boolean mandatory) {
        String raw = props.getProperty(key);
        if (raw == null && mandatory) {
            throw new IllegalStateException(key + " property is mandatory but not found");
        }

        return Optional.fromNullable(Converters.convert(raw, clazz));
    }

    @Override
    public Class<?> getObjectType() {
        return DataSource.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void destroy() throws Exception {
        if (dataSource != null && dataSource instanceof BasicDataSource) {
            ((BasicDataSource) dataSource).close();
        }
        dataSource = null;
    }
}
