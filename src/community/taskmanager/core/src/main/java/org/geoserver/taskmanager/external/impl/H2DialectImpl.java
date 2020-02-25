/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external.impl;

/**
 * H2 Dialect.
 *
 * @author Timothy De Bock
 */
public class H2DialectImpl extends DefaultDialectImpl {

    /** Do not quote table names since this not supported by H2. */
    @Override
    public String quote(String tableName) {
        return tableName;
    }

    @Override
    public String sqlRenameView(String currentViewName, String newViewName) {
        return "ALTER TABLE " + currentViewName + " RENAME TO " + newViewName;
    }
}
