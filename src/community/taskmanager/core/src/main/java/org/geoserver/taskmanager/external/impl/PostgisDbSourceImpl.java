/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external.impl;

import it.geosolutions.geoserver.rest.encoder.GSAbstractStoreEncoder;
import it.geosolutions.geoserver.rest.encoder.datastore.GSPostGISDatastoreEncoder;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.geoserver.taskmanager.external.DbSource;
import org.geoserver.taskmanager.external.DbTable;
import org.geoserver.taskmanager.external.Dialect;
import org.geoserver.taskmanager.external.ExternalGS;
import org.geoserver.taskmanager.util.SecuredImpl;
import org.geoserver.taskmanager.util.SqlUtil;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * DbSource for Postgres.
 *
 * @author Niels Charlier
 */
public class PostgisDbSourceImpl extends SecuredImpl implements DbSource {

    private String host;

    private int port = 5432;

    private String db;

    private boolean ssl = false;

    private String schema;

    private String username;

    private String password;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDb() {
        return db;
    }

    public void setDb(String db) {
        this.db = db;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
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
    public DataSource getDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        String url = "jdbc:postgresql://" + host + ":" + port + "/" + db;
        if (schema != null) {
            url += "?currentSchema=" + schema + ",public";
        }
        if (ssl) {
            url += (schema == null ? "?" : "&") + "sslmode=require";
        }
        dataSource.setUrl(url);
        return dataSource;
    }

    @Override
    public GSAbstractStoreEncoder getStoreEncoder(String name, ExternalGS extGs) {
        GSPostGISDatastoreEncoder encoder = new GSPostGISDatastoreEncoder(name);
        encoder.setHost(host);
        encoder.setPort(port);
        encoder.setDatabase(db);
        encoder.setSchema(schema);
        encoder.setUser(username);
        encoder.setPassword(password);
        return encoder;
    }

    @Override
    public Map<String, Serializable> getParameters() {
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put(PostgisNGDataStoreFactory.DBTYPE.key, "postgis");
        params.put(PostgisNGDataStoreFactory.HOST.key, host);
        params.put(PostgisNGDataStoreFactory.PORT.key, port);
        params.put(PostgisNGDataStoreFactory.DATABASE.key, db);
        params.put(PostgisNGDataStoreFactory.SCHEMA.key, schema);
        params.put(PostgisNGDataStoreFactory.USER.key, username);
        params.put(PostgisNGDataStoreFactory.PASSWD.key, password);
        return params;
    }

    @Override
    public GSAbstractStoreEncoder postProcess(GSAbstractStoreEncoder encoder, DbTable table) {
        if (table != null) {
            String schema = SqlUtil.schema(table.getTableName());
            if (schema != null) {
                ((GSPostGISDatastoreEncoder) encoder).setSchema(schema);
            }
        }
        return encoder;
    }

    @Override
    public Dialect getDialect() {
        return new PostgisDialectImpl();
    }

    /*
    @Override
    public InputStream dump(String realTableName, String tempTableName) throws IOException {
        String url = "jdbc:postgresql://" + username + ":" + password + "@" + host + ":" + port + "/" + db;
        if (ssl) {
            url +=  "?sslmode=require";
        }
        Process pr = Runtime.getRuntime().exec(
                "pg_dump --dbname=" + url + " --table " + (schema == null ? "" : schema + ".") + realTableName);

        //to do: remove the search_path from the script
        //+ replace all names (table, sequences, indexes, constraints) to temporary names

        return pr.getInputStream();
    }

    @Override
    public OutputStream script() throws IOException {
        String url = "jdbc:postgresql://" + username + ":" + password + "@" + host + ":" + port + "/" + db;
        if (ssl) {
            url +=  "?sslmode=require";
        }
        if (schema != null) {
            url +=  (ssl ? "&" : "?") + "options=--search_path%3D" + schema;
        }
        Process pr = Runtime.getRuntime().exec("psql --dbname=" + url);
        return pr.getOutputStream();
    }*/

}
