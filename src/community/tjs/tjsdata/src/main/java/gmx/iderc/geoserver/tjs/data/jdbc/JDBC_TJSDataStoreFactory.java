/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.data.jdbc;


import gmx.iderc.geoserver.tjs.data.AbstractTJSDataStoreFactory;
import org.apache.commons.dbcp.BasicDataSource;
import org.geotools.data.DataAccessFactory.Param;
import org.geotools.data.Parameter;
import org.geotools.util.SimpleInternationalString;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * @author root
 */
public abstract class JDBC_TJSDataStoreFactory extends AbstractTJSDataStoreFactory {

    /**
     * parameter for database type
     */
    public static final Param DBTYPE = new Param("dbtype", String.class, "Type", true);

    /**
     * parameter for database host
     */
    public static final Param HOST = new Param("host", String.class, "Host", true, "localhost");

    /**
     * parameter for database port
     */
    public static final Param PORT = new Param("port", Integer.class, "Port", true);

    /**
     * parameter for database instance
     */
    public static final Param DATABASE = new Param("database", String.class, "Database", true);

    /**
     * parameter for database schema
     */
    public static final Param SCHEMA = new Param("schema", String.class, "Schema", false);

    /**
     * parameter for database user
     */
    public static final Param USER = new Param("user", String.class,
                                                      "user name to login as");

    /**
     * parameter for database password
     */
    public static final Param PASSWD = new Param("passwd", String.class,
                                                        new SimpleInternationalString("password used to login"), false, null, Collections
                                                                                                                                      .singletonMap(Parameter.IS_PASSWORD, Boolean.TRUE));

    /**
     * parameter for data source
     */
    public static final Param DATASOURCE = new Param("Data Source", DataSource.class, "Data Source", false);

    /**
     * parameter for data source
     */
    public static final Param DATASOURCENAME = new Param("Data Source Name", String.class, "Data Source Name", true);

    /**
     * Maximum number of connections in the connection pool
     */
    public static final Param MAXCONN = new Param("max connections", Integer.class,
                                                         "maximum number of open connections", false, new Integer(10));

    /**
     * Minimum number of connections in the connection pool
     */
    public static final Param MINCONN = new Param("min connections", Integer.class,
                                                         "minimum number of pooled connection", false, new Integer(1));

    /**
     * If connections should be validated before using them
     */
    public static final Param FETCHSIZE = new Param("fetch size", Integer.class,
                                                           "number of records read with each iteraction with the dbms", false, 1000);

    /**
     * Maximum amount of time the pool will wait when trying to grab a new connection *
     */
    public static final Param MAXWAIT = new Param("Connection timeout", Integer.class,
                                                         "number of seconds the connection pool will wait before timing out attempting to get a new connection (default, 20 seconds)", false, 20);

    @Override
    public String getDisplayName() {
        return getDatabaseID();
    }

    @Override
    public boolean canProcess(Map params) {
        if (!super.canProcess(params)) {
            return false; // was not in agreement with getParametersInfo
        }

        return checkDBType(params);
    }

    protected boolean checkDBType(Map params) {
        return checkDBType(params, getDatabaseID());
    }

    protected final boolean checkDBType(Map params, String dbtype) {
        String type;

        try {
            type = (String) DBTYPE.lookUp(params);

            if (dbtype.equals(type)) {
                return true;
            }

            return false;
        } catch (IOException e) {
            return false;
        }
    }

    public final Param[] getParametersInfo() {
        LinkedHashMap map = new LinkedHashMap();
        setupParameters(map);

        return (Param[]) map.values().toArray(new Param[map.size()]);
    }

    /**
     * Sets up the database connection parameters.
     * <p>
     * Subclasses may extend, but should not override. This implementation
     * registers the following parameters.
     * <ul>
     * <li>{@link #HOST}
     * <li>{@link #PORT}
     * <li>{@link #DATABASE}
     * <li>{@link #SCHEMA}
     * <li>{@link #USER}
     * <li>{@link #PASSWD}
     * </ul>
     * Subclass implementation may remove any parameters from the map, or may
     * overrwrite any parameters in the map.
     * </p>
     *
     * @param parameters Map of {@link Param} objects.
     */
    protected void setupParameters(Map parameters) {
        // remember: when adding a new parameter here that is not connection related,
        // add it to the JDBCJNDIDataStoreFactory class
        parameters.put(DBTYPE.key,
                              new Param(DBTYPE.key, DBTYPE.type, DBTYPE.description, DBTYPE.required, getDatabaseID()));
        parameters.put(HOST.key, HOST);
        parameters.put(PORT.key, PORT);
        parameters.put(DATABASE.key, DATABASE);
        parameters.put(SCHEMA.key, SCHEMA);
        parameters.put(USER.key, USER);
        parameters.put(PASSWD.key, PASSWD);
        parameters.put(MAXCONN.key, MAXCONN);
        parameters.put(MINCONN.key, MINCONN);
        parameters.put(FETCHSIZE.key, FETCHSIZE);
        parameters.put(MAXWAIT.key, MAXWAIT);
    }

    /**
     * Determines if the datastore is available.
     * <p>
     * Subclasses may with to override or extend this method. This implementation
     * checks whether the jdbc driver class (provided by {@link #getDriverClassName()}
     * can be loaded.
     * </p>
     */
    public boolean isAvailable() {
        try {
            Class.forName(getDriverClassName());

            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Returns the implementation hints for the datastore.
     * <p>
     * Subclasses may override, this implementation returns <code>null</code>.
     * </p>
     */
    public Map<java.awt.RenderingHints.Key, ?> getImplementationHints() {
        return null;
    }

    /**
     * Returns a string to identify the type of the database.
     * <p>
     * Example: 'postgis'.
     * </p>
     */
    protected abstract String getDatabaseID();

    /**
     * Returns the fully qualified class name of the jdbc driver.
     * <p>
     * For example: org.postgresql.Driver
     * </p>
     */
    protected abstract String getDriverClassName();

//    /**
//     * Creates the dialect that the datastore uses for communication with the
//     * underlying database.
//     *
//     * @param dataStore The datastore.
//     */
//    protected abstract SQLDialect createSQLDialect(JDBCDataStore dataStore);


    /**
     * DataSource access allowing SQL use: intended to allow client code to query available schemas.
     * <p>
     * This DataSource is the clients responsibility to close() when they are finished using it.
     * </p>
     *
     * @param params Map of connection parameter.
     * @return DataSource for SQL use
     * @throws IOException
     */
    public BasicDataSource createDataSource(Map params) throws IOException {

        //create a datasource
        BasicDataSource dataSource = new BasicDataSource();

        // driver
        dataSource.setDriverClassName(getDriverClassName());

        // url
        dataSource.setUrl(getJDBCUrl(params));

        // username
        String user = (String) USER.lookUp(params);
        dataSource.setUsername(user);

        // password
        String passwd = (String) PASSWD.lookUp(params);
        if (passwd != null) {
            dataSource.setPassword(passwd);
        }

        // max wait
        Integer maxWait = (Integer) MAXWAIT.lookUp(params);
        if (maxWait != null && maxWait != -1) {
            dataSource.setMaxWait(maxWait * 1000);
        }

        // connection pooling options
        Integer minConn = (Integer) MINCONN.lookUp(params);
        if (minConn != null) {
            dataSource.setMinIdle(minConn);
        }

        Integer maxConn = (Integer) MAXCONN.lookUp(params);
        if (maxConn != null) {
            dataSource.setMaxActive(maxConn);
        }

        // some datastores might need this
        dataSource.setAccessToUnderlyingConnectionAllowed(true);
        return dataSource;
    }

    /**
     * Builds up the JDBC url in a jdbc:<database>://<host>:<port>/<dbname>
     * Override if you need a different setup
     *
     * @param params
     * @return
     * @throws IOException
     */
    protected String getJDBCUrl(Map params) throws IOException {
        // jdbc url
        String host = (String) HOST.lookUp(params);
        Integer port = (Integer) PORT.lookUp(params);
        String db = (String) DATABASE.lookUp(params);

        String url = "jdbc:" + getDatabaseID() + "://" + host;
        if (port != null) {
            url += ":" + port;
        }

        if (db != null) {
            url += "/" + db;
        }
        return url;
    }

    @Override
    public Map<String, Serializable> filterParamsForSave(Map params) {
        HashMap<String, Serializable> saveMap = new HashMap<String, Serializable>();
        for (Iterator i = params.keySet().iterator(); i.hasNext(); ) {
            String key = i.next().toString();
            if (key.equals(DATASOURCE.key)) {
                continue;
            }
            Object o = params.get(key);
            if (o instanceof Serializable) {
                saveMap.put(key, (Serializable) o);
            }
        }
        return saveMap;
    }


}
