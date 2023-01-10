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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.jdbcconfig.JDBCConfigTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;

/**
 * @author groldan
 * @author Kevin Smith, OpenGeo
 */
public class QueryBuilderTest {

    private JDBCConfigTestSupport testSupport;

    private DbMappings dbMappings;

    Dialect dialect;

    @Before
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

    @After
    public void tearDown() throws Exception {
        testSupport.tearDown();
    }

    @Test
    public void testQueryAll() {
        Filter filter = Predicates.equal("name", "ws1");
        StringBuilder build =
                QueryBuilder.forIds(dialect, WorkspaceInfo.class, dbMappings)
                        .filter(filter)
                        .build();
    }

    @Test
    public void testSort1() {
        Filter filter = Predicates.acceptAll();
        StringBuilder build =
                QueryBuilder.forIds(dialect, WorkspaceInfo.class, dbMappings)
                        .filter(filter)
                        .sortOrder(Predicates.asc("foo"))
                        .build();
    }

    @Test
    public void testSort2() {
        Filter filter = Predicates.acceptAll();
        StringBuilder build =
                QueryBuilder.forIds(dialect, WorkspaceInfo.class, dbMappings)
                        .filter(filter)
                        .sortOrder(Predicates.asc("foo"), Predicates.desc("bar"))
                        .build();
    }

    @Test
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

    @Test
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
        assertThat(
                sql,
                containsString(
                        "NOT (oid IN (SELECT oid FROM object_property WHERE property_type IN (:ptype0) AND UPPER(value) = :value0)"));
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
        assertThat(sql, containsString("type_id = " + dbMappings.getTypeId(LayerInfo.class)));
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
                "oid IN (SELECT oid FROM object_property WHERE property_type IN (:"
                        + "ptype0) AND value IS NULL) OR oid NOT IN (SELECT oid FROM object_property WHERE property_type IN (:"
                        + "ptype0))";
        // Ensure the following sql is present
        assertThat(sql, containsString(sqlNull));
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
                "oid IN (SELECT oid FROM object_property WHERE property_type IN (:"
                        + "ptype0) AND value IS NULL)";
        // Ensure the following sql is present
        assertThat(sql, containsString(sqlNil));
    }
}
