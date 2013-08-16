package org.geoserver.jdbcconfig.internal;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.geotools.factory.GeoTools;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

import com.google.common.base.Optional;

public class DataSourceFactoryBean implements FactoryBean<DataSource>, DisposableBean {

    private static final Logger LOGGER = Logging.getLogger(DataSourceFactoryBean.class);

    JDBCConfigProperties config;
    Context jndiCtx;
    DataSource dataSource;
    
    private static Context getJNDI() {
        try {
            return GeoTools.getInitialContext(GeoTools.getDefaultHints());
        } catch (NamingException ex) {
            LOGGER.log(Level.WARNING, "Could not get JNDI Context, will not use JNDI to locate DataSource", ex);
            return null;
        }
    }
    
    public DataSourceFactoryBean(JDBCConfigProperties config) {
        this(config, getJNDI());
    }

    
    public DataSourceFactoryBean(JDBCConfigProperties config, Context jndiCtx) {
        this.config = config;
        this.jndiCtx = jndiCtx;
    }

    @Override
    public DataSource getObject() throws Exception {
        if (dataSource == null) {
            dataSource = getDataSource();
        }
        return dataSource;
    }
    
    /**
     * Look up or create a DataSource
     * @return
     * @throws Exception
     */
    protected DataSource getDataSource() throws Exception {
        DataSource ds = getJNDIDataSource(get(config, "jndiName", String.class, false)).orNull();
        if(ds==null) {
            ds = createDataSource();
        }
        return ds;
    }
    
    /**
     * Get an unconfigured BasicDataSource to set up
     * @return
     */
    protected BasicDataSource createBasicDataSource() {
        return new BasicDataSource();
    }
    
    /**
     * Try to lookup a configured DataSource using JNDI.
     * @return
     * @throws NamingException 
     */
    protected Optional<DataSource> getJNDIDataSource(Optional<String> name) {
        if(jndiCtx==null) return Optional.absent();
        
        if(name.isPresent()) {
            try {
                Optional<DataSource> ds =  Optional.of((DataSource)jndiCtx.lookup(name.get()));
                if(LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.log(Level.INFO, "JDBCConfig using JNDI DataSource {0}", name.get());
                }
                return ds;
            } catch (NamingException ex) {
                if(LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(Level.WARNING, "Could not resolve JNDI name "+name.get()+" for JDBCConfig Database", ex);
                }
                return Optional.absent();
            }
        } else {
            if(LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("No JNDI name given for JDBCConfig DB.");
            }
            return Optional.absent();
        }
    }
    
    /**
     * Create and configure a DataSource based on the JDBCConfigProperties
     * @return
     * @throws Exception
     */
    protected DataSource createDataSource() throws Exception {
        BasicDataSource dataSource = createBasicDataSource();

        dataSource.setUrl(config.getJdbcUrl());

        Optional<String> driverClassName = get(config, "driverClassName", String.class, true);
        try {
            Class.forName(driverClassName.get());
        }
        catch(Exception e) {
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

        if(LOGGER.isLoggable(Level.INFO)) {
            LOGGER.log(Level.INFO, "JDBCConfig using JDBC DataSource {0}", config.getJdbcUrl());
        }

        //TODO: more connection parameters
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
            ((BasicDataSource)dataSource).close();
        }
        dataSource = null;
    } 
}
