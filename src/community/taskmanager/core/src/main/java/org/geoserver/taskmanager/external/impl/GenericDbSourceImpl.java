/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external.impl;

import it.geosolutions.geoserver.rest.encoder.GSAbstractStoreEncoder;
import java.io.Serializable;
import java.util.Map;
import javax.sql.DataSource;
import org.geoserver.taskmanager.external.DbSource;
import org.geoserver.taskmanager.external.DbTable;
import org.geoserver.taskmanager.external.Dialect;
import org.geoserver.taskmanager.external.ExternalGS;
import org.geoserver.taskmanager.util.SecuredImpl;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Generic datasource that only takes the connection url and driver class as input in addition the
 * the user name and password. The schema attribute is optional.
 *
 * @author Timothy De Bock
 */
public class GenericDbSourceImpl extends SecuredImpl implements DbSource {

    private String connectionUrl;

    private String driver;

    private String username;

    private String password;

    private String schema;

    public String getConnectionUrl() {
        return connectionUrl;
    }

    public void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    @Override
    public DataSource getDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(driver);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setUrl(connectionUrl);
        return dataSource;
    }

    @Override
    public GSAbstractStoreEncoder getStoreEncoder(String name, ExternalGS extGs) {
        throw new UnsupportedOperationException("Generic datasource cannot be used as a store.");
    }

    @Override
    public Map<String, Serializable> getParameters() {
        throw new UnsupportedOperationException("Generic datasource cannot be used as a store.");
    }

    @Override
    public GSAbstractStoreEncoder postProcess(GSAbstractStoreEncoder encoder, DbTable table) {
        throw new UnsupportedOperationException("Generic datasource cannot be used as a store.");
    }

    @Override
    public Dialect getDialect() {
        return new DefaultDialectImpl();
    }
}
