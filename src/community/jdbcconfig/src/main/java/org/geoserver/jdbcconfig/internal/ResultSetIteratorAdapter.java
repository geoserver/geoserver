/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import com.google.common.base.Throwables;
import java.sql.Connection;
import java.sql.ResultSet;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;

public class ResultSetIteratorAdapter<T extends CatalogInfo> implements CloseableIterator<T> {

    private ResultSet resultSet;

    private Connection connection;

    private final RowMapper<T> rowMapper;

    private boolean hasNext;

    public ResultSetIteratorAdapter(
            final Connection connection, final ResultSet resultSet, final RowMapper<T> rowMapper) {

        this.connection = connection;
        this.resultSet = resultSet;
        this.rowMapper = rowMapper;
        try {
            this.hasNext = resultSet.next();
        } catch (Exception e) {
            Throwables.throwIfUnchecked(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public T next() {
        T next;
        try {
            next = rowMapper.mapRow(resultSet, 0);
            this.hasNext = resultSet.next();
        } catch (Exception e) {
            Throwables.throwIfUnchecked(e);
            throw new RuntimeException(e);
        }
        return next;
    }

    @Override
    public void close() {
        if (resultSet != null) {
            JdbcUtils.closeResultSet(resultSet);
            resultSet = null;
            JdbcUtils.closeConnection(connection);
            connection = null;
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void finalize() {
        if (resultSet != null) {
            try {
                close();
            } finally {
                ConfigDatabase.LOGGER.warning(
                        "There is code not closing CloseableIterator!!! Auto closing at finalize().");
            }
        }
    }
}
