/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external;

import it.geosolutions.geoserver.rest.encoder.GSAbstractStoreEncoder;
import org.geoserver.taskmanager.util.Named;


import javax.sql.DataSource;
import java.util.Map;

/**
 * A database configuration used by tasks.
 * 
 * @author Niels Charlier
 *
 */
public interface DbSource extends Named {
    
    /**
     * Get a data source for this database.
     * 
     * @return the data source.
     */
    DataSource getDataSource();
    
    /**
     * Get a geoserver store encoder from this source.
     * 
     * @param name name for the source
     * 
     * @return the geoserver store encoder
     */
    GSAbstractStoreEncoder getStoreEncoder(String name);

    /**
     * Generate parameters for GeoServer datastore
     * 
     * @return the parameters for GeoServer datastore
     */
    Map<String, Object> getParameters();

    /**
     * schema 
     * 
     * @return
     */
    String getSchema();
    
    /**
     * 
     * 
     * @param encoder
     * @param origParameters
     * @return 
     */
    GSAbstractStoreEncoder postProcess(GSAbstractStoreEncoder encoder, DbTable table);
    
    /*
     * these methods could serve an alternative table copy implementation
     * that doesn't use jdbc but uses direct database commands and sends SQL commands
     * through a pipeline between servers. 
     * 
    public InputStream dump(String realTableName, String tempTableName) throws IOException;

    OutputStream script() throws IOException;
     */



    /**
     * The dialect specific actions for taskmanager.
     *
     * @return
     */
    Dialect getDialect();
}
