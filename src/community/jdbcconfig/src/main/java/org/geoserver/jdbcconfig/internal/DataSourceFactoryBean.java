package org.geoserver.jdbcconfig.internal;

import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.geotools.util.Converters;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

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

        String driverClassName = get(config, "driverClassName", String.class, true, null);
        try {
            Class.forName(driverClassName);
        }
        catch(Exception e) {
            throw new RuntimeException("Error loading jdbc driver class: " + driverClassName, e);
        }
        
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUsername(get(config, "username", String.class, false, null));
        dataSource.setPassword(get(config, "password", String.class, false, null));

        //TODO: rest of connection parameters
        return dataSource;
    }

    <T> T get(Properties props, String key, Class<T> clazz, boolean mandatory, T def) {
        String raw = props.getProperty(key);
        if (raw == null && mandatory) {
            throw new IllegalStateException(key + " property is mandatory but not found");
        }
        if (raw == null) {
            return def;
        }

        return Converters.convert(raw, clazz);
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
