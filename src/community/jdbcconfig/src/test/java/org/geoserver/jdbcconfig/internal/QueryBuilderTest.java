/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
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
package org.geoserver.jdbcconfig.internal;

import junit.framework.TestCase;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.jdbcconfig.JDBCConfigTestSupport;
import org.junit.Test;
import org.opengis.filter.Filter;

/**
 * @author groldan
 * @author Kevin Smith, OpenGeo
 */
public class QueryBuilderTest extends TestCase {

    private JDBCConfigTestSupport testSupport;

    private DbMappings dbMappings;

    Dialect dialect;

    public void setUp() throws Exception {
        dialect = new Dialect();
        dbMappings = new DbMappings(dialect);
        testSupport =
                new JDBCConfigTestSupport(
                        (JDBCConfigTestSupport.DBConfig)
                                JDBCConfigTestSupport.parameterizedDBConfigs().get(0)[0]);
        testSupport.setUp();
        dbMappings = testSupport.getDbMappings();
    }

    public void tearDown() throws Exception {
        testSupport.tearDown();
    }

    public void testQueryAll() {
        Filter filter = Predicates.equal("name", "ws1");
        StringBuilder build =
                QueryBuilder.forIds(dialect, WorkspaceInfo.class, dbMappings)
                        .filter(filter)
                        .build();
    }

    public void testSort1() {
        Filter filter = Predicates.acceptAll();
        StringBuilder build =
                QueryBuilder.forIds(dialect, WorkspaceInfo.class, dbMappings)
                        .filter(filter)
                        .sortOrder(Predicates.asc("foo"))
                        .build();
    }

    public void testSort2() {
        Filter filter = Predicates.acceptAll();
        StringBuilder build =
                QueryBuilder.forIds(dialect, WorkspaceInfo.class, dbMappings)
                        .filter(filter)
                        .sortOrder(Predicates.asc("foo"), Predicates.desc("bar"))
                        .build();
    }

    public void testSort3() {
        Filter filter = Predicates.acceptAll();
        StringBuilder build =
                QueryBuilder.forIds(dialect, WorkspaceInfo.class, dbMappings)
                        .filter(filter)
                        .sortOrder(
                                Predicates.asc("foo"),
                                Predicates.desc("bar"),
                                Predicates.asc("baz"))
                        .build();
    }

    public void testSort3WithFilter() {
        Filter filter = Predicates.equal("name", "quux");
        StringBuilder build =
                QueryBuilder.forIds(dialect, WorkspaceInfo.class, dbMappings)
                        .filter(filter)
                        .sortOrder(
                                Predicates.asc("foo"),
                                Predicates.desc("bar"),
                                Predicates.asc("baz"))
                        .build();
    }

    @Test
    public void testNotEquals() {
        // Create the filter
        Filter filter = Predicates.notEqual("name", "quux");
        // Build it
        StringBuilder build =
                QueryBuilder.forIds(dialect, WorkspaceInfo.class, dbMappings)
                        .filter(filter)
                        .build();
        String sql = build.toString();
        // Ensure the following sql is present
        assertTrue(
                sql.contains(
                        "NOT oid IN (SELECT oid FROM object_property WHERE property_type IN (:ptype0) AND UPPER(value) = :value0)"));
    }

    @Test
    public void testIsInstanceOf() {
        // Create the filter
        Filter filter = Predicates.isInstanceOf(LayerInfo.class);
        // Build it
        StringBuilder build =
                QueryBuilder.forIds(dialect, WorkspaceInfo.class, dbMappings)
                        .filter(filter)
                        .build();
        String sql = build.toString();
        // Ensure the following sql is present
        assertTrue(sql.contains("type_id = " + dbMappings.getTypeId(LayerInfo.class)));
    }

    @Test
    public void testIsNull() {
        // Create the filter
        Filter filter = Predicates.isNull("name");
        // Build it
        StringBuilder build =
                QueryBuilder.forIds(dialect, WorkspaceInfo.class, dbMappings)
                        .filter(filter)
                        .build();
        String sql = build.toString();

        String sqlNull =
                "oid IN (select oid from object_property where property_type in (:"
                        + "ptype0) and value IS NULL) OR oid NOT  in (select oid from object_property where property_type in (:"
                        + "ptype0))";
        // Ensure the following sql is present
        assertTrue(sql.contains(sqlNull));
    }

    @Test
    public void testIsNil() {
        // Create the filter
        Filter filter = Predicates.isNull("name");
        // Build it
        StringBuilder build =
                QueryBuilder.forIds(dialect, WorkspaceInfo.class, dbMappings)
                        .filter(filter)
                        .build();
        String sql = build.toString();

        String sqlNil =
                "oid IN (select oid from object_property where property_type in (:"
                        + "ptype0) and value IS NULL)";
        // Ensure the following sql is present
        assertTrue(sql.contains(sqlNil));
    }
}
