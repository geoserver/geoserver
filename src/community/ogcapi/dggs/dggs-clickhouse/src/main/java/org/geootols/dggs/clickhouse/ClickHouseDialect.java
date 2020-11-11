/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geootols.dggs.clickhouse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.geotools.data.jdbc.FilterToSQL;
import org.geotools.jdbc.BasicSQLDialect;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.type.GeometryDescriptor;

class ClickHouseDialect extends BasicSQLDialect {

    private static final String GEOMETRY_NOT_SUPPORTED =
            "Geometry functions are not supported in ClickHouse, this store is provided as pure JDBC support to DGGS";

    protected ClickHouseDialect(JDBCDataStore dataStore) {
        super(dataStore);
    }

    @Override
    public void encodeGeometryValue(Geometry value, int dimension, int srid, StringBuffer sql)
            throws IOException {
        throw new UnsupportedOperationException(GEOMETRY_NOT_SUPPORTED);
    }

    @Override
    public void encodeGeometryEnvelope(String tableName, String geometryColumn, StringBuffer sql) {
        throw new UnsupportedOperationException(GEOMETRY_NOT_SUPPORTED);
    }

    @Override
    public Envelope decodeGeometryEnvelope(ResultSet rs, int column, Connection cx)
            throws SQLException, IOException {
        throw new UnsupportedOperationException(GEOMETRY_NOT_SUPPORTED);
    }

    @Override
    public Geometry decodeGeometryValue(
            GeometryDescriptor descriptor,
            ResultSet rs,
            String column,
            GeometryFactory factory,
            Connection cx,
            Hints hints)
            throws IOException, SQLException {
        throw new UnsupportedOperationException(GEOMETRY_NOT_SUPPORTED);
    }

    @Override
    public FilterToSQL createFilterToSQL() {
        FilterToSQL f2s = new ClickHouseFilterToSQL();
        f2s.setCapabilities(BASE_DBMS_CAPABILITIES);
        return f2s;
    }

    @Override
    public boolean isLimitOffsetSupported() {
        return true;
    }

    @Override
    public void applyLimitOffset(StringBuffer sql, int limit, int offset) {
        if (limit >= 0 && limit < Integer.MAX_VALUE) {
            sql.append(" LIMIT " + limit);
            if (offset > 0) {
                sql.append(" OFFSET " + offset);
            }
        } else if (offset > 0) {
            sql.append(" OFFSET " + offset);
        }
    }
}
