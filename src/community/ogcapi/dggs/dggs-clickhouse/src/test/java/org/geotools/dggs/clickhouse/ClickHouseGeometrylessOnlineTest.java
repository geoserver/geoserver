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

import java.util.Map;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.jdbc.JDBCGeometrylessOnlineTest;
import org.geotools.jdbc.JDBCGeometrylessTestSetup;

@SuppressWarnings("PMD.JUnit4TestShouldUseTestAnnotation") // JUnit 3 tests here
public class ClickHouseGeometrylessOnlineTest extends JDBCGeometrylessOnlineTest {
    @Override
    protected JDBCGeometrylessTestSetup createTestSetup() {
        return new ClickHouseGeometrylessTestSetup(new ClickHouseJDBCTestSetup());
    }

    @Override
    protected Map<String, Object> createDataStoreFactoryParams() throws Exception {
        Map<String, Object> params = super.createDataStoreFactoryParams();
        params.put(JDBCDataStoreFactory.SCHEMA.key, params.get(JDBCDataStoreFactory.DATABASE.key));
        return params;
    }

    @Override
    public void testPersonSchema() throws Exception {
        // ignore, // there are differences in nullability handling
    }

    @Override
    public void testCreate() throws Exception {
        // ignore, does not work, no interest in table creation for the moment
    }

    @Override
    public void testWriteFeatures() throws Exception {
        // no support for auto-generated keys, but this test assumes them
    }
}
