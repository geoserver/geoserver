/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcstore;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.geoserver.jdbcstore.internal.JDBCResourceStoreProperties;

/**
 * @author Kevin Smith, Boundless
 * @author Niels Charlier
 */
public interface DatabaseTestSupport extends AutoCloseable {

    /** Ensure the database in initialised with the JDBCStore schema */
    public void initialize() throws Exception;

    /**
     * Add a file
     *
     * @param name The name of the file
     * @param parent The OID of the parent directory
     * @param content The content of the file
     * @return the OID of the new file
     */
    public int addFile(String name, int parent, byte[] content) throws SQLException;

    /**
     * Add a directory
     *
     * @param name The name of the directory
     * @param parent The OID of the parent directory
     * @return the OID of the new directory
     */
    public int addDir(String name, int parent) throws SQLException;

    /** Get the OID of the root node */
    public int getRoot();

    /** Get the data source */
    public DataSource getDataSource();

    /** Get a connection to the data source */
    public Connection getConnection() throws SQLException;

    /** Close any open resources */
    public void close() throws SQLException;

    /**
     * Stub the database relevant configuration options of the provided easyMock
     * JDBCResourceStoreProperties
     */
    public abstract void stubConfig(JDBCResourceStoreProperties config);
}
