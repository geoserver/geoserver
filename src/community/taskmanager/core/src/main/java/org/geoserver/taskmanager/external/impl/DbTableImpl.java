/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external.impl;

import org.geoserver.taskmanager.external.DbSource;
import org.geoserver.taskmanager.external.DbTable;

/**
 * Represents a table in a db (implementation).
 *
 * @author Niels Charlier
 */
public class DbTableImpl implements DbTable {

    private DbSource dbSource;

    private String tableName;

    public DbTableImpl(DbSource dbSource, String tableName) {
        this.dbSource = dbSource;
        this.tableName = tableName;
    }

    @Override
    public DbSource getDbSource() {
        return dbSource;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dbSource == null) ? 0 : dbSource.hashCode());
        result = prime * result + ((tableName == null) ? 0 : tableName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        DbTableImpl other = (DbTableImpl) obj;
        if (dbSource == null) {
            if (other.dbSource != null) return false;
        } else if (!dbSource.equals(other.dbSource)) return false;
        if (tableName == null) {
            if (other.tableName != null) return false;
        } else if (!tableName.equals(other.tableName)) return false;
        return true;
    }
}
