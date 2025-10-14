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
package org.geotools.dggs.clickhouse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.geotools.api.feature.FeatureVisitor;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.data.jdbc.FilterToSQL;
import org.geotools.jdbc.BasicSQLDialect;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

class ClickHouseDialect extends BasicSQLDialect {

    private static final String GEOMETRY_NOT_SUPPORTED =
            "Geometry functions are not supported in ClickHouse, this store is provided as pure JDBC support to DGGS";

    protected ClickHouseDialect(JDBCDataStore dataStore) {
        super(dataStore);
    }

    @Override
    public void encodeGeometryValue(Geometry value, int dimension, int srid, StringBuffer sql) throws IOException {
        throw new UnsupportedOperationException(GEOMETRY_NOT_SUPPORTED);
    }

    @Override
    public void encodeGeometryEnvelope(String tableName, String geometryColumn, StringBuffer sql) {
        throw new UnsupportedOperationException(GEOMETRY_NOT_SUPPORTED);
    }

    @Override
    public Envelope decodeGeometryEnvelope(ResultSet rs, int column, Connection cx) throws SQLException, IOException {
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

    @Override
    public Object convertValue(Object value, AttributeDescriptor ad) {
        Class<?> binding = ad.getType().getBinding();
        Date c = convertValue(value, binding);
        if (c != null) return c;
        return super.convertValue(value, ad);
    }

    private static Date convertValue(Object value, Class<?> binding) {
        if (value instanceof LocalDate d && binding.equals(java.sql.Date.class)) {
            Calendar c = Calendar.getInstance();
            c.clear();
            c.set(d.getYear(), d.getMonthValue() - 1, d.getDayOfMonth(), 0, 0, 0);
            return new java.sql.Date(c.getTimeInMillis());
        } else if (value instanceof LocalDateTime time && binding.equals(java.sql.Timestamp.class)) {
            return java.sql.Timestamp.from(time.atZone(ZoneId.systemDefault()).toInstant());
        }
        return null;
    }

    /** SQLite dates are just strings, they don't get converted to Date in case of aggregation, do it here instead */
    @Override
    public Function<Object, Object> getAggregateConverter(FeatureVisitor visitor, SimpleFeatureType featureType) {
        Optional<List<Class>> maybeResultTypes = getResultTypes(visitor, featureType);
        if (maybeResultTypes.isPresent()) {
            List<Class> resultTypes = maybeResultTypes.get();
            if (resultTypes.size() == 1) {
                Class<?> targetType = resultTypes.get(0);
                if (java.util.Date.class.isAssignableFrom(targetType)) {
                    return v -> {
                        return convertValue(v, targetType);
                    };
                }
            }
        }
        // otherwise no conversion needed
        return Function.identity();
    }
}
