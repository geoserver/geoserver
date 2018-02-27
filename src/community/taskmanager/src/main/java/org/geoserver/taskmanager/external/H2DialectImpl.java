/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external;

import java.sql.Connection;
import java.util.Collections;
import java.util.Set;

/**
 * H2 Dialect.
 * 
 * @author Timothy De Bock
 */
public class H2DialectImpl extends DefaultDialectImpl {

    /**
     * Do not quote table names since this not supported by H2.
     *
     * @param tableName
     * @return
     */
    @Override
    public String quote(String tableName) {
        return tableName;
    }

    @Override
    public String sqlRenameView(String currentViewName, String newViewName) {
        return "ALTER TABLE " + currentViewName + " RENAME TO " + newViewName;
    }


    @Override
    public Set<String> getSpatialColumns(Connection sourceConn, String tableName) {
        return Collections.emptySet();
    }
}
