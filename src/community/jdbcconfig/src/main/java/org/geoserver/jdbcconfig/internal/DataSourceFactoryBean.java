package org.geoserver.jdbcconfig.internal;

import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.geotools.util.Converters;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

import com.google.common.base.Optional;

public class DataSourceFactoryBean implements FactoryBean<DataSource>, DisposableBean {

    JDBCConfigProperties config;
    DataSource dataSource;

    public DataSourceFactoryBean(JDBCConfigProperties config) {
        this.config = config;
    }

    @Override
    public DataSource getObject() throws Exception {
        if (dataSource == null) {
            dataSource = createDataSource();
        }
        return dataSource;
    }

    protected DataSource createDataSource() throws Exception {
        BasicDataSource dataSource = new BasicDataSource();

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
