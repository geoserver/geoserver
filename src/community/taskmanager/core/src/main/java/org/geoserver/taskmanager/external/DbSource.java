/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external;

import it.geosolutions.geoserver.rest.encoder.GSAbstractStoreEncoder;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import org.geoserver.taskmanager.util.Secured;

/**
 * A database configuration used by tasks.
 *
 * @author Niels Charlier
 */
public interface DbSource extends Secured {

    /**
     * Get a data source for this database.
     *
     * @return the data source.
     */
    DataSource getDataSource() throws SQLException;

    /**
     * Get a geoserver store encoder from this source.
     *
     * @param name name for the source
     * @param extGs TODO
     * @return the geoserver store encoder
     */
    GSAbstractStoreEncoder getStoreEncoder(String name, ExternalGS extGs);

    /**
     * Generate parameters for GeoServer datastore
     *
     * @return the parameters for GeoServer datastore
     */
    Map<String, Serializable> getParameters();

    /** schema */
    String getSchema();

    /** */
    GSAbstractStoreEncoder postProcess(GSAbstractStoreEncoder encoder, DbTable table);

    /** The dialect specific actions for taskmanager. */
    Dialect getDialect();

    /*
     * these methods could serve an alternative table copy implementation
     * that doesn't use jdbc but uses direct database commands and sends SQL commands
     * through a pipeline between servers.
     *
    public InputStream dump(String realTableName, String tempTableName) throws IOException;

    OutputStream script() throws IOException;
     */
}
