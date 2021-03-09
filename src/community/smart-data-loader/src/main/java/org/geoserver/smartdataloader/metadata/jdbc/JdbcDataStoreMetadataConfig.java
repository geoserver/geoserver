/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.metadata.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.smartdataloader.metadata.DataStoreMetadataConfig;
import org.geoserver.smartdataloader.metadata.jdbc.utils.JdbcUrlSplitter;
import org.geotools.data.Transaction;
import org.geotools.jdbc.JDBCDataStore;

/**
 * Configuration class that keeps specific information related to DataStoreMetadata for JDBCs
 * connections.
 */
public class JdbcDataStoreMetadataConfig extends DataStoreMetadataConfig {

    public static final String TYPE = "JDBC";

    private final Connection connection;
    private String password;
    private final String name;
    private final String catalog;
    private final String schema;

    public JdbcDataStoreMetadataConfig(JDBCDataStore jdbcStore, String password)
            throws IOException, SQLException {
        this.connection = jdbcStore.getConnection(Transaction.AUTO_COMMIT);
        this.name = jdbcStore.getDatabaseSchema();
        this.catalog = jdbcStore.getDataSource().getConnection().getCatalog();
        this.schema = jdbcStore.getDatabaseSchema();
        // required as parameter since it cannot be extracted from jdbc api
        this.password = password;
    }

    public JdbcDataStoreMetadataConfig(
            String name, Connection connection, String catalog, String schema) {
        super();
        this.name = name;
        this.connection = connection;
        this.catalog = catalog;
        this.schema = schema;
    }

    public Connection getConnection() throws IOException {
        return connection;
    }

    public String getCatalog() {
        return catalog;
    }

    public String getSchema() {
        return schema;
    }

    @Override
    public String getType() {
        return JdbcDataStoreMetadataConfig.TYPE;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("Type: ");
        stringBuilder.append(this.getType());
        stringBuilder.append(" - Connection: ");
        try {
            stringBuilder.append(this.connection.getMetaData().getURL());
        } catch (SQLException e) {
            throw new RuntimeException("JdbcDataStoreMetadataConfig: Cannot get connection URL.");
        }
        stringBuilder.append(" - Catalog: ");
        stringBuilder.append(this.getCatalog());
        stringBuilder.append(" - Schema: ");
        stringBuilder.append(this.getSchema());
        return stringBuilder.toString();
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> out = new HashMap<>();
        try {
            JdbcUrlSplitter urlFields = new JdbcUrlSplitter(connection.getMetaData().getURL());
            String dbtype = urlFields.driverName;
            String host = urlFields.host;
            String port = urlFields.port;
            String database = urlFields.database;
            String username = connection.getMetaData().getUserName();
            String password = this.getPassword();
            // in case dbtype = postgresql, then translate it to postgis (since geoserver would not
            // understand it in
            // appschema context
            if (dbtype.equals("postgresql")) dbtype = "postgis";

            out.put("dbtype", dbtype);
            out.put("host", host);
            out.put("port", port);
            out.put("database", database);
            out.put("schema", schema);
            out.put("user", username);
            out.put("passwd", password);
            // need to force it to true, even when wrapped datastore does not defines that
            out.put("Expose primary keys", "true");

        } catch (SQLException e) {
            throw new RuntimeException("Error gettings URL parameters from JDBC connection.");
        }
        return out;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public String getPassword() {
        return password;
    }
}
