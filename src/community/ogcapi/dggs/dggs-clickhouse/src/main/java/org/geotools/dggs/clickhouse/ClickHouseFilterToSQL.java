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
import java.text.SimpleDateFormat;
import java.util.Date;
import org.geotools.data.jdbc.FilterToSQL;

public class ClickHouseFilterToSQL extends FilterToSQL {

    @Override
    protected void writeLiteral(Object literal) throws IOException {
        if (literal instanceof java.sql.Date) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String isoDate = sdf.format(((Date) literal));

            out.write("toDate('" + isoDate + "')");
        } else if (literal instanceof Date) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            String isoDate = sdf.format(((Date) literal));

            out.write("parseDateTimeBestEffort('" + isoDate + "')");
        } else {
            super.writeLiteral(literal);
        }
    }
}
