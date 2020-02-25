/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external.impl;

import java.sql.Connection;

/**
 * Informix 11 implementation.
 *
 * @author Timothy De Bock
 */
public class InformixDialectImpl extends DefaultDialectImpl {

    @Override
    public String quote(String tableName) {
        return tableName;
    }

    /**
     * Override because in a view informix returns the value of the underlying column definition of
     * the table. Even when performing left join in the create view statement.
     */
    @Override
    public int isNullable(int nullable) {
        return -1;
    }

    /** No schemas in informix 11. */
    @Override
    public String createSchema(Connection connection, String schema) {
        return "";
    }
}
