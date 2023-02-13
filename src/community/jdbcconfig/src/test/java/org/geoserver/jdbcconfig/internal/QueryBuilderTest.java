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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.jdbcconfig.JDBCConfigTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.sort.SortBy;

/**
 * @author groldan
 * @author Kevin Smith, OpenGeo
 */
public class QueryBuilderTest {

    private static final FilterFactory FACTORY = Predicates.factory;

    private JDBCConfigTestSupport testSupport;

    private DbMappings dbMappings;

    Dialect dialect;

    @Before
    public void setUp() throws Exception {
        dialect = new Dialect();
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
    public void testForIdsSort1DebugDisabled() {
        String expected =
                "SELECT id FROM (SELECT oid, id FROM object WHERE type_id IN (:types)) object "
                        + "LEFT JOIN (SELECT oid, value prop0 FROM object_property "
                        + "WHERE property_type IN (:sortProperty0)) subSelect0 ON object.oid = subSelect0.oid "
                        + "ORDER BY prop0 ASC";
        verifyForIds(expected, false, Predicates.acceptAll(), Predicates.asc("foo"));
    }

    @Test
    public void testForIdsSort1DebugEnabled() {
        String expected =
                "SELECT id FROM"
                        + "\n    (SELECT oid, id FROM object WHERE type_id IN (:types) /* org.geoserver.catalog.WorkspaceInfo */"
                        + "\n) object"
                        + "\n  LEFT JOIN"
                        + "\n    (SELECT oid, value prop0 FROM"
                        + "\n      object_property WHERE property_type IN (:sortProperty0)) subSelect0 /* foo ASC */"
                        + "\n  ON object.oid = subSelect0.oid"
                        + "\n  ORDER BY prop0 ASC";
        verifyForIds(expected, true, Predicates.acceptAll(), Predicates.asc("foo"));
    }

    @Test
    public void testForIdsSort2DebugDisabled() {
        String expected =
                "SELECT id FROM (SELECT oid, id FROM object WHERE type_id IN (:types)) object "
                        + "LEFT JOIN (SELECT oid, value prop0 FROM object_property "
                        + "WHERE property_type IN (:sortProperty0)) subSelect0 ON object.oid = subSelect0.oid "
                        + "LEFT JOIN (SELECT oid, value prop1 FROM object_property "
                        + "WHERE property_type IN (:sortProperty1)) subSelect1 ON object.oid = subSelect1.oid "
                        + "ORDER BY prop0 ASC, prop1 DESC";
        verifyForIds(
                expected,
                false,
                Predicates.acceptAll(),
                Predicates.asc("foo"),
                Predicates.desc("bar"));
    }

    @Test
    public void testForIdsSort2DebugEnabled() {
        String expected =
                "SELECT id FROM"
                        + "\n    (SELECT oid, id FROM object WHERE type_id IN (:types) /* org.geoserver.catalog.WorkspaceInfo */"
                        + "\n) object"
                        + "\n  LEFT JOIN"
                        + "\n    (SELECT oid, value prop0 FROM"
                        + "\n      object_property WHERE property_type IN (:sortProperty0)) subSelect0 /* foo ASC */"
                        + "\n  ON object.oid = subSelect0.oid"
                        + "\n  LEFT JOIN"
                        + "\n    (SELECT oid, value prop1 FROM"
                        + "\n      object_property WHERE property_type IN (:sortProperty1)) subSelect1 /* bar DESC */"
                        + "\n  ON object.oid = subSelect1.oid"
                        + "\n  ORDER BY prop0 ASC, prop1 DESC";
        verifyForIds(
                expected,
                true,
                Predicates.acceptAll(),
                Predicates.asc("foo"),
                Predicates.desc("bar"));
    }

    @Test
    public void testForIdsSort3DebugDisabled() {
        String expected =
                "SELECT id FROM (SELECT oid, id FROM object WHERE type_id IN (:types)) object "
                        + "LEFT JOIN (SELECT oid, value prop0 FROM object_property "
                        + "WHERE property_type IN (:sortProperty0)) subSelect0 ON object.oid = subSelect0.oid "
                        + "LEFT JOIN (SELECT oid, value prop1 FROM object_property "
                        + "WHERE property_type IN (:sortProperty1)) subSelect1 ON object.oid = subSelect1.oid "
                        + "LEFT JOIN (SELECT oid, value prop2 FROM object_property "
                        + "WHERE property_type IN (:sortProperty2)) subSelect2 ON object.oid = subSelect2.oid "
                        + "ORDER BY prop0 ASC, prop1 DESC, prop2 ASC";
        verifyForIds(
                expected,
                false,
                Predicates.acceptAll(),
                Predicates.asc("foo"),
                Predicates.desc("bar"),
                Predicates.asc("baz"));
    }

    @Test
    public void testForIdsSort3DebugEnabled() {
        String expected =
                "SELECT id FROM"
                        + "\n    (SELECT oid, id FROM object WHERE type_id IN (:types) /* org.geoserver.catalog.WorkspaceInfo */"
                        + "\n) object"
                        + "\n  LEFT JOIN"
                        + "\n    (SELECT oid, value prop0 FROM"
                        + "\n      object_property WHERE property_type IN (:sortProperty0)) subSelect0 /* foo ASC */"
                        + "\n  ON object.oid = subSelect0.oid"
                        + "\n  LEFT JOIN"
                        + "\n    (SELECT oid, value prop1 FROM"
                        + "\n      object_property WHERE property_type IN (:sortProperty1)) subSelect1 /* bar DESC */"
                        + "\n  ON object.oid = subSelect1.oid"
                        + "\n  LEFT JOIN"
                        + "\n    (SELECT oid, value prop2 FROM"
                        + "\n      object_property WHERE property_type IN (:sortProperty2)) subSelect2 /* baz ASC */"
                        + "\n  ON object.oid = subSelect2.oid"
                        + "\n  ORDER BY prop0 ASC, prop1 DESC, prop2 ASC";
        verifyForIds(
                expected,
                true,
                Predicates.acceptAll(),
                Predicates.asc("foo"),
                Predicates.desc("bar"),
                Predicates.asc("baz"));
    }

    @Test
    public void testForIdsSort3WithFilterDebugDisabled() {
        String expected =
                "SELECT id FROM (SELECT oid, id FROM object WHERE type_id IN (:types) AND oid IN "
                        + "(SELECT oid FROM object_property WHERE property_type IN (:ptype0) AND value = :value0)) object "
                        + "LEFT JOIN (SELECT oid, value prop0 FROM object_property "
                        + "WHERE property_type IN (:sortProperty0)) subSelect0 ON object.oid = subSelect0.oid "
                        + "LEFT JOIN (SELECT oid, value prop1 FROM object_property "
                        + "WHERE property_type IN (:sortProperty1)) subSelect1 ON object.oid = subSelect1.oid "
                        + "LEFT JOIN (SELECT oid, value prop2 FROM object_property "
                        + "WHERE property_type IN (:sortProperty2)) subSelect2 ON object.oid = subSelect2.oid "
                        + "ORDER BY prop0 ASC, prop1 DESC, prop2 ASC";
        verifyForIds(
                expected,
                false,
                Predicates.equal("name", "quux"),
                Predicates.asc("foo"),
                Predicates.desc("bar"),
                Predicates.asc("baz"));
    }

    @Test
    public void testForIdsSort3WithFilterDebugEnabled() {
        String expected =
                "SELECT id FROM"
                        + "\n    (SELECT oid, id FROM object WHERE type_id IN (:types) /* org.geoserver.catalog.WorkspaceInfo */"
                        + "\n      AND oid IN (SELECT oid FROM object_property WHERE property_type IN (:ptype0) AND value = :value0) /* [ name = quux ] */"
                        + "\n) object"
                        + "\n  LEFT JOIN"
                        + "\n    (SELECT oid, value prop0 FROM"
                        + "\n      object_property WHERE property_type IN (:sortProperty0)) subSelect0 /* foo ASC */"
                        + "\n  ON object.oid = subSelect0.oid"
                        + "\n  LEFT JOIN"
                        + "\n    (SELECT oid, value prop1 FROM"
                        + "\n      object_property WHERE property_type IN (:sortProperty1)) subSelect1 /* bar DESC */"
                        + "\n  ON object.oid = subSelect1.oid"
                        + "\n  LEFT JOIN"
                        + "\n    (SELECT oid, value prop2 FROM"
                        + "\n      object_property WHERE property_type IN (:sortProperty2)) subSelect2 /* baz ASC */"
                        + "\n  ON object.oid = subSelect2.oid"
                        + "\n  ORDER BY prop0 ASC, prop1 DESC, prop2 ASC";
        verifyForIds(
                expected,
                true,
                Predicates.equal("name", "quux"),
                Predicates.asc("foo"),
                Predicates.desc("bar"),
                Predicates.asc("baz"));
    }

    @Test
    public void testForCountUnknownProperty() {
        dialect.setDebugMode(false);
        String expected = "SELECT COUNT(oid) FROM object WHERE type_id IN (:types)";
        Filter filter = Predicates.equal("foo.bar.baz", "quux");
        QueryBuilder<?> builder =
                QueryBuilder.forCount(dialect, WorkspaceInfo.class, dbMappings).filter(filter);
        String actual = builder.build();
        assertEquals(expected, actual);
        assertEquals(Filter.INCLUDE, builder.getSupportedFilter());
        assertEquals(filter, builder.getUnsupportedFilter());
        assertFalse(builder.isOffsetLimitApplied());
        assertEquals(1, builder.getNamedParameters().size());
    }

    @Test
    public void testForIdsUnknownProperty() {
        dialect.setDebugMode(false);
        String expected = "SELECT id FROM object WHERE type_id IN (:types) ORDER BY oid";
        Filter filter = Predicates.equal("foo.bar.baz", "quux");
        QueryBuilder<?> builder =
                QueryBuilder.forIds(dialect, WorkspaceInfo.class, dbMappings).filter(filter);
        String actual = builder.build();
        assertEquals(expected, actual);
        assertEquals(Filter.INCLUDE, builder.getSupportedFilter());
        assertEquals(filter, builder.getUnsupportedFilter());
        assertFalse(builder.isOffsetLimitApplied());
        assertEquals(1, builder.getNamedParameters().size());
    }

    @Test
    public void testForCountSimplifiedInclude() {
        dialect.setDebugMode(false);
        String expected = "SELECT COUNT(oid) FROM object WHERE type_id IN (:types)";
        Filter filter = Predicates.and(Predicates.acceptAll(), Predicates.acceptAll());
        QueryBuilder<?> builder =
                QueryBuilder.forCount(dialect, WorkspaceInfo.class, dbMappings).filter(filter);
        String actual = builder.build();
        assertEquals(expected, actual);
        assertEquals(Filter.INCLUDE, builder.getSupportedFilter());
        assertEquals(Filter.INCLUDE, builder.getUnsupportedFilter());
        assertFalse(builder.isOffsetLimitApplied());
        assertEquals(1, builder.getNamedParameters().size());
    }

    @Test
    public void testForIdsSimplifiedInclude() {
        dialect.setDebugMode(false);
        String expected = "SELECT id FROM object WHERE type_id IN (:types) ORDER BY oid";
        Filter filter = Predicates.and(Predicates.acceptAll(), Predicates.acceptAll());
        QueryBuilder<?> builder =
                QueryBuilder.forIds(dialect, WorkspaceInfo.class, dbMappings).filter(filter);
        String actual = builder.build();
        assertEquals(expected, actual);
        assertEquals(Filter.INCLUDE, builder.getSupportedFilter());
        assertEquals(Filter.INCLUDE, builder.getUnsupportedFilter());
        assertTrue(builder.isOffsetLimitApplied());
        assertEquals(1, builder.getNamedParameters().size());
    }

    @Test
    public void testForIdsIncludeWithOffSetDebugDisabled() {
        dialect.setDebugMode(false);
        String expected =
                "SELECT id FROM object WHERE type_id IN (:types) ORDER BY oid LIMIT 2147483647 OFFSET 5";
        String actual =
                QueryBuilder.forIds(dialect, WorkspaceInfo.class, dbMappings).offset(5).build();
        assertEquals(expected, actual);
    }

    @Test
    public void testForIdsIncludeWithLimitDebugDisabled() {
        dialect.setDebugMode(false);
        String expected = "SELECT id FROM object WHERE type_id IN (:types) ORDER BY oid LIMIT 10";
        String actual =
                QueryBuilder.forIds(dialect, WorkspaceInfo.class, dbMappings).limit(10).build();
        assertEquals(expected, actual);
    }

    @Test
    public void testForIdsIncludeWithOffSetAndLimitDebugDisabled() {
        dialect.setDebugMode(false);
        String expected =
                "SELECT id FROM object WHERE type_id IN (:types) ORDER BY oid LIMIT 10 OFFSET 5";
        String actual =
                QueryBuilder.forIds(dialect, WorkspaceInfo.class, dbMappings)
                        .offset(5)
                        .limit(10)
                        .build();
        assertEquals(expected, actual);
    }

    @Test
    public void testForIdsIncludeWithOffSetDebugEnabled() {
        dialect.setDebugMode(true);
        String expected =
                "SELECT id FROM object WHERE type_id IN (:types) /* org.geoserver.catalog.WorkspaceInfo */"
                        + "\nORDER BY oid LIMIT 2147483647 OFFSET 5";
        String actual =
                QueryBuilder.forIds(dialect, WorkspaceInfo.class, dbMappings).offset(5).build();
        assertEquals(expected, actual);
    }

    @Test
    public void testForIdsIncludeWithLimitDebugEnabled() {
        dialect.setDebugMode(true);
        String expected =
                "SELECT id FROM object WHERE type_id IN (:types) /* org.geoserver.catalog.WorkspaceInfo */"
                        + "\nORDER BY oid LIMIT 10";
        String actual =
                QueryBuilder.forIds(dialect, WorkspaceInfo.class, dbMappings).limit(10).build();
        assertEquals(expected, actual);
    }

    @Test
    public void testForIdsIncludeWithOffSetAndLimitDebugEnabled() {
        dialect.setDebugMode(true);
        String expected =
                "SELECT id FROM object WHERE type_id IN (:types) /* org.geoserver.catalog.WorkspaceInfo */"
                        + "\nORDER BY oid LIMIT 10 OFFSET 5";
        String actual =
                QueryBuilder.forIds(dialect, WorkspaceInfo.class, dbMappings)
                        .offset(5)
                        .limit(10)
                        .build();
        assertEquals(expected, actual);
    }

    @Test
    public void testForCountIncludeDebugDisabled() {
        String expected = "SELECT COUNT(oid) FROM object WHERE type_id IN (:types)";
        verifyForCount(expected, false, Predicates.acceptAll());
    }

    @Test
    public void testForCountIncludeDebugEnabled() {
        String expected =
                "SELECT COUNT(oid) FROM object WHERE type_id IN (:types) /* org.geoserver.catalog.WorkspaceInfo */";
        verifyForCount(expected, true, Predicates.acceptAll());
    }

    @Test
    public void testForIdsIncludeDebugDisabled() {
        String expected = "SELECT id FROM object WHERE type_id IN (:types) ORDER BY oid";
        verifyForIds(expected, false, Predicates.acceptAll());
    }

    @Test
    public void testForIdsIncludeDebugEnabled() {
        String expected =
                "SELECT id FROM object WHERE type_id IN (:types) /* org.geoserver.catalog.WorkspaceInfo */"
                        + "\nORDER BY oid";
        verifyForIds(expected, true, Predicates.acceptAll());
    }

    @Test
    public void testForCountAndIsInstanceofDebugDisabled() {
        String expected = "(type_id = 14 AND 0 = 1)";
        Filter filter =
                Predicates.and(
                        Predicates.isInstanceOf(LayerInfo.class),
                        Predicates.isInstanceOf(String.class));
        verifyForCount(expected, false, filter);
    }

    @Test
    public void testForCountAndIsInstanceofDebugEnabled() {
        String expected =
                "(\n    type_id = 14 /* isInstanceOf org.geoserver.catalog.LayerInfo */"
                        + "\n    AND\n    0 = 1 /* EXCLUDE */\n)";
        Filter filter =
                Predicates.and(
                        Predicates.isInstanceOf(LayerInfo.class),
                        Predicates.isInstanceOf(String.class));
        verifyForCount(expected, true, filter);
    }

    @Test
    public void testForIdsAndIsInstanceofDebugDisabled() {
        String expected = "(type_id = 14 AND 0 = 1) ";
        Filter filter =
                Predicates.and(
                        Predicates.isInstanceOf(LayerInfo.class),
                        Predicates.isInstanceOf(String.class));
        verifyForIds(expected, false, filter);
    }

    @Test
    public void testForIdsAndIsInstanceofDebugEnabled() {
        String expected =
                "(\n    type_id = 14 /* isInstanceOf org.geoserver.catalog.LayerInfo */"
                        + "\n    AND\n    0 = 1 /* EXCLUDE */\n) ";
        Filter filter =
                Predicates.and(
                        Predicates.isInstanceOf(LayerInfo.class),
                        Predicates.isInstanceOf(String.class));
        verifyForIds(expected, true, filter);
    }

    @Test
    public void testForCountOrIsInstanceofDebugDisabled() {
        String expected = "(type_id = 14 OR 0 = 1)";
        Filter filter =
                Predicates.or(
                        Predicates.isInstanceOf(LayerInfo.class),
                        Predicates.isInstanceOf(String.class));
        verifyForCount(expected, false, filter);
    }

    @Test
    public void testForCountOrIsInstanceofDebugEnabled() {
        String expected =
                "(\n    type_id = 14 /* isInstanceOf org.geoserver.catalog.LayerInfo */"
                        + "\n    OR\n    0 = 1 /* EXCLUDE */\n)";
        Filter filter =
                Predicates.or(
                        Predicates.isInstanceOf(LayerInfo.class),
                        Predicates.isInstanceOf(String.class));
        verifyForCount(expected, true, filter);
    }

    @Test
    public void testForIdsOrIsInstanceofDebugDisabled() {
        String expected = "(type_id = 14 OR 0 = 1) ";
        Filter filter =
                Predicates.or(
                        Predicates.isInstanceOf(LayerInfo.class),
                        Predicates.isInstanceOf(String.class));
        verifyForIds(expected, false, filter);
    }

    @Test
    public void testForIdsOrIsInstanceofDebugEnabled() {
        String expected =
                "(\n    type_id = 14 /* isInstanceOf org.geoserver.catalog.LayerInfo */"
                        + "\n    OR\n    0 = 1 /* EXCLUDE */\n) ";
        Filter filter =
                Predicates.or(
                        Predicates.isInstanceOf(LayerInfo.class),
                        Predicates.isInstanceOf(String.class));
        verifyForIds(expected, true, filter);
    }

    @Test
    public void testForCountIsEqualToSensitiveDebugDisabled() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND value = :value0)";
        Filter filter = FACTORY.equal(FACTORY.property("name"), FACTORY.literal("quux"), true);
        verifyForCount(expected, false, filter);
    }

    @Test
    public void testForCountIsEqualToSensitiveDebugEnabled() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND value = :value0) /* [ name = quux ] */";
        Filter filter = FACTORY.equal(FACTORY.property("name"), FACTORY.literal("quux"), true);
        verifyForCount(expected, true, filter);
    }

    @Test
    public void testForCountIsEqualToSensitiveDebugEnabledEscaping() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND value = :value0) /* [ name = FOO*\\/BAR ] */";
        Filter filter = FACTORY.equal(FACTORY.property("name"), FACTORY.literal("FOO*/BAR"), true);
        verifyForCount(expected, true, filter);
    }

    @Test
    public void testForIdsIsEqualToSensitiveDebugDisabled() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND value = :value0) ";
        Filter filter = FACTORY.equal(FACTORY.property("name"), FACTORY.literal("quux"), true);
        verifyForIds(expected, false, filter);
    }

    @Test
    public void testForIdsIsEqualToSensitiveDebugEnabled() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND value = :value0) /* [ name = quux ] */\n";
        Filter filter = FACTORY.equal(FACTORY.property("name"), FACTORY.literal("quux"), true);
        verifyForIds(expected, true, filter);
    }

    @Test
    public void testForIdsIsEqualToSensitiveDebugEnabledEscaping() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND value = :value0) /* [ name = FOO*\\/BAR ] */\n";
        Filter filter = FACTORY.equal(FACTORY.property("name"), FACTORY.literal("FOO*/BAR"), true);
        verifyForIds(expected, true, filter);
    }

    @Test
    public void testForCountIsEqualToInsensitiveDebugDisabled() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND UPPER(value) = :value0)";
        Filter filter = FACTORY.equal(FACTORY.property("name"), FACTORY.literal("quux"), false);
        verifyForCount(expected, false, filter);
    }

    @Test
    public void testForCountIsEqualToInsensitiveDebugEnabled() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND UPPER(value) = :value0) /* [ name = quux ] */";
        Filter filter = FACTORY.equal(FACTORY.property("name"), FACTORY.literal("quux"), false);
        verifyForCount(expected, true, filter);
    }

    @Test
    public void testForCountIsEqualToInsensitiveDebugEnabledEscaping() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND UPPER(value) = :value0) /* [ name = FOO*\\/BAR ] */";
        Filter filter = FACTORY.equal(FACTORY.property("name"), FACTORY.literal("FOO*/BAR"), false);
        verifyForCount(expected, true, filter);
    }

    @Test
    public void testForIdsIsEqualToInsensitiveDebugDisabled() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND UPPER(value) = :value0) ";
        Filter filter = FACTORY.equal(FACTORY.property("name"), FACTORY.literal("quux"), false);
        verifyForIds(expected, false, filter);
    }

    @Test
    public void testForIdsIsEqualToInsensitiveDebugEnabled() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND UPPER(value) = :value0) /* [ name = quux ] */\n";
        Filter filter = FACTORY.equal(FACTORY.property("name"), FACTORY.literal("quux"), false);
        verifyForIds(expected, true, filter);
    }

    @Test
    public void testForIdsIsEqualToInsensitiveDebugEnabledEscaping() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND UPPER(value) = :value0) /* [ name = FOO*\\/BAR ] */\n";
        Filter filter = FACTORY.equal(FACTORY.property("name"), FACTORY.literal("FOO*/BAR"), false);
        verifyForIds(expected, true, filter);
    }

    @Test
    public void testForCountIsNotEqualToSensitiveDebugDisabled() {
        String expected =
                "NOT (oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND value = :value0))";
        Filter filter = FACTORY.notEqual(FACTORY.property("name"), FACTORY.literal("quux"), true);
        verifyForCount(expected, false, filter);
    }

    @Test
    public void testForCountIsNotEqualToSensitiveDebugEnabled() {
        String expected =
                "NOT (oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND value = :value0) /* [ name = quux ] */\n)";
        Filter filter = FACTORY.notEqual(FACTORY.property("name"), FACTORY.literal("quux"), true);
        verifyForCount(expected, true, filter);
    }

    @Test
    public void testForCountIsNotEqualToSensitiveDebugEnabledEscaping() {
        String expected =
                "NOT (oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND value = :value0) /* [ name = FOO*\\/BAR ] */\n)";
        Filter filter =
                FACTORY.notEqual(FACTORY.property("name"), FACTORY.literal("FOO*/BAR"), true);
        verifyForCount(expected, true, filter);
    }

    @Test
    public void testForIdsIsNotEqualToSensitiveDebugDisabled() {
        String expected =
                "NOT (oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND value = :value0)) ";
        Filter filter = FACTORY.notEqual(FACTORY.property("name"), FACTORY.literal("quux"), true);
        verifyForIds(expected, false, filter);
    }

    @Test
    public void testForIdsIsNotEqualToSensitiveDebugEnabled() {
        String expected =
                "NOT (oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND value = :value0) /* [ name = quux ] */\n) ";
        Filter filter = FACTORY.notEqual(FACTORY.property("name"), FACTORY.literal("quux"), true);
        verifyForIds(expected, true, filter);
    }

    @Test
    public void testForIdsIsNotEqualToSensitiveDebugEnabledEscaping() {
        String expected =
                "NOT (oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND value = :value0) /* [ name = FOO*\\/BAR ] */\n) ";
        Filter filter =
                FACTORY.notEqual(FACTORY.property("name"), FACTORY.literal("FOO*/BAR"), true);
        verifyForIds(expected, true, filter);
    }

    @Test
    public void testForCountIsNotEqualToInsensitiveDebugDisabled() {
        String expected =
                "NOT (oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND UPPER(value) = :value0))";
        Filter filter = FACTORY.notEqual(FACTORY.property("name"), FACTORY.literal("quux"), false);
        verifyForCount(expected, false, filter);
    }

    @Test
    public void testForCountIsNotEqualToInsensitiveDebugEnabled() {
        String expected =
                "NOT (oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND UPPER(value) = :value0) /* [ name = quux ] */\n)";
        Filter filter = FACTORY.notEqual(FACTORY.property("name"), FACTORY.literal("quux"), false);
        verifyForCount(expected, true, filter);
    }

    @Test
    public void testForCountIsNotEqualToInsensitiveDebugEnabledEscaping() {
        String expected =
                "NOT (oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND UPPER(value) = :value0) /* [ name = FOO*\\/BAR ] */\n)";
        Filter filter =
                FACTORY.notEqual(FACTORY.property("name"), FACTORY.literal("FOO*/BAR"), false);
        verifyForCount(expected, true, filter);
    }

    @Test
    public void testForIdsIsNotEqualToInsensitiveDebugDisabled() {
        String expected =
                "NOT (oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND UPPER(value) = :value0)) ";
        Filter filter = FACTORY.notEqual(FACTORY.property("name"), FACTORY.literal("quux"), false);
        verifyForIds(expected, false, filter);
    }

    @Test
    public void testForIdsIsNotEqualToInsensitiveDebugEnabled() {
        String expected =
                "NOT (oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND UPPER(value) = :value0) /* [ name = quux ] */\n) ";
        Filter filter = FACTORY.notEqual(FACTORY.property("name"), FACTORY.literal("quux"), false);
        verifyForIds(expected, true, filter);
    }

    @Test
    public void testForIdsIsNotEqualToInsensitiveDebugEnabledEscaping() {
        String expected =
                "NOT (oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND UPPER(value) = :value0) /* [ name = FOO*\\/BAR ] */\n) ";
        Filter filter =
                FACTORY.notEqual(FACTORY.property("name"), FACTORY.literal("FOO*/BAR"), false);
        verifyForIds(expected, true, filter);
    }

    @Test
    public void testForCountIsLikeSensitiveDebugDisabled() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND value LIKE :value0)";
        Filter filter = FACTORY.like(FACTORY.property("name"), "%quux%", "%", "_", "\\", true);
        verifyForCount(expected, false, filter);
    }

    @Test
    public void testForCountIsLikeSensitiveDebugDisabledEscaping() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND value LIKE :value0)";
        Filter filter = FACTORY.like(FACTORY.property("name"), "%\\'FOO%", "%", "_", "\\", true);
        verifyForCount(expected, false, filter);
    }

    @Test
    public void testForCountIsLikeSensitiveDebugEnabled() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND value LIKE :value0) /* [ name is like %quux% ] */";
        Filter filter = FACTORY.like(FACTORY.property("name"), "%quux%", "%", "_", "\\", true);
        verifyForCount(expected, true, filter);
    }

    @Test
    public void testForCountIsLikeSensitiveDebugEnabledEscaping1() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND value LIKE :value0) /* [ name is like %\\'FOO% ] */";
        Filter filter = FACTORY.like(FACTORY.property("name"), "%\\'FOO%", "%", "_", "\\", true);
        verifyForCount(expected, true, filter);
    }

    @Test
    public void testForCountIsLikeSensitiveDebugEnabledEscaping2() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND value LIKE :value0) /* [ name is like %FOO*\\/BAR% ] */";
        Filter filter = FACTORY.like(FACTORY.property("name"), "%FOO*/BAR%", "%", "_", "\\", true);
        verifyForCount(expected, true, filter);
    }

    @Test
    public void testForIdsIsLikeSensitiveDebugDisabled() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND value LIKE :value0) ";
        Filter filter = FACTORY.like(FACTORY.property("name"), "%quux%", "%", "_", "\\", true);
        verifyForIds(expected, false, filter);
    }

    @Test
    public void testForIdsIsLikeSensitiveDebugDisabledEscaping() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND value LIKE :value0) ";
        Filter filter = FACTORY.like(FACTORY.property("name"), "%\\'FOO%", "%", "_", "\\", true);
        verifyForIds(expected, false, filter);
    }

    @Test
    public void testForIdsIsLikeSensitiveDebugEnabled() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND value LIKE :value0) /* [ name is like %quux% ] */\n";
        Filter filter = FACTORY.like(FACTORY.property("name"), "%quux%", "%", "_", "\\", true);
        verifyForIds(expected, true, filter);
    }

    @Test
    public void testForIdsIsLikeSensitiveDebugEnabledEscaping1() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND value LIKE :value0) /* [ name is like %\\'FOO% ] */\n";
        Filter filter = FACTORY.like(FACTORY.property("name"), "%\\'FOO%", "%", "_", "\\", true);
        verifyForIds(expected, true, filter);
    }

    @Test
    public void testForIdsIsLikeSensitiveDebugEnabledEscaping2() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND value LIKE :value0) /* [ name is like %FOO*\\/BAR% ] */\n";
        Filter filter = FACTORY.like(FACTORY.property("name"), "%FOO*/BAR%", "%", "_", "\\", true);
        verifyForIds(expected, true, filter);
    }

    @Test
    public void testForCountIsLikeInsensitiveDebugDisabled() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND UPPER(value) LIKE :value0)";
        Filter filter = FACTORY.like(FACTORY.property("name"), "%quux%", "%", "_", "\\", false);
        verifyForCount(expected, false, filter);
    }

    @Test
    public void testForCountIsLikeInsensitiveDebugDisabledEscaping() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND UPPER(value) LIKE :value0)";
        Filter filter = FACTORY.like(FACTORY.property("name"), "%\\'FOO%", "%", "_", "\\", false);
        verifyForCount(expected, false, filter);
    }

    @Test
    public void testForCountIsLikeInsensitiveDebugEnabled() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND UPPER(value) LIKE :value0) /* [ name is like %quux% ] */";
        Filter filter = FACTORY.like(FACTORY.property("name"), "%quux%", "%", "_", "\\", false);
        verifyForCount(expected, true, filter);
    }

    @Test
    public void testForCountIsLikeInsensitiveDebugEnabledEscaping1() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND UPPER(value) LIKE :value0) /* [ name is like %\\'FOO% ] */";
        Filter filter = FACTORY.like(FACTORY.property("name"), "%\\'FOO%", "%", "_", "\\", false);
        verifyForCount(expected, true, filter);
    }

    @Test
    public void testForCountIsLikeInsensitiveDebugEnabledEscaping2() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND UPPER(value) LIKE :value0) /* [ name is like %FOO*\\/BAR% ] */";
        Filter filter = FACTORY.like(FACTORY.property("name"), "%FOO*/BAR%", "%", "_", "\\", false);
        verifyForCount(expected, true, filter);
    }

    @Test
    public void testForIdsIsLikeInsensitiveDebugDisabled() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND UPPER(value) LIKE :value0) ";
        Filter filter = FACTORY.like(FACTORY.property("name"), "%quux%", "%", "_", "\\", false);
        verifyForIds(expected, false, filter);
    }

    @Test
    public void testForIdsIsLikeInsensitiveDebugDisabledEscaping() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND UPPER(value) LIKE :value0) ";
        Filter filter = FACTORY.like(FACTORY.property("name"), "%\\'FOO%", "%", "_", "\\", false);
        verifyForIds(expected, false, filter);
    }

    @Test
    public void testForIdsIsLikeInsensitiveDebugEnabled() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND UPPER(value) LIKE :value0) /* [ name is like %quux% ] */\n";
        Filter filter = FACTORY.like(FACTORY.property("name"), "%quux%", "%", "_", "\\", false);
        verifyForIds(expected, true, filter);
    }

    @Test
    public void testForIdsIsLikeInsensitiveDebugEnabledEscaping1() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND UPPER(value) LIKE :value0) /* [ name is like %\\'FOO% ] */\n";
        Filter filter = FACTORY.like(FACTORY.property("name"), "%\\'FOO%", "%", "_", "\\", false);
        verifyForIds(expected, true, filter);
    }

    @Test
    public void testForIdsIsLikeInsensitiveDebugEnabledEscaping2() {
        String expected =
                "oid IN (SELECT oid FROM object_property WHERE property_type "
                        + "IN (:ptype0) AND UPPER(value) LIKE :value0) /* [ name is like %FOO*\\/BAR% ] */\n";
        Filter filter = FACTORY.like(FACTORY.property("name"), "%FOO*/BAR%", "%", "_", "\\", false);
        verifyForIds(expected, true, filter);
    }

    @Test
    public void testForCountIsNullDebugDisabled() {
        String expected =
                "(oid IN (SELECT oid FROM object_property WHERE property_type IN (:"
                        + "ptype0) AND value IS NULL) OR oid NOT IN (SELECT oid FROM object_property WHERE property_type IN (:"
                        + "ptype0)))";
        verifyForCount(expected, false, Predicates.isNull("name"));
    }

    @Test
    public void testForCountIsNullDebugEnabled() {
        String expected =
                "(oid IN (SELECT oid FROM object_property WHERE property_type IN (:"
                        + "ptype0) AND value IS NULL) OR oid NOT IN (SELECT oid FROM object_property WHERE property_type IN (:"
                        + "ptype0))) /* [ name IS NULL ] */";
        verifyForCount(expected, true, Predicates.isNull("name"));
    }

    @Test
    public void testForIdsIsNullDebugDisabled() {
        String expected =
                "(oid IN (SELECT oid FROM object_property WHERE property_type IN (:"
                        + "ptype0) AND value IS NULL) OR oid NOT IN (SELECT oid FROM object_property WHERE property_type IN (:"
                        + "ptype0))) ";
        verifyForIds(expected, false, Predicates.isNull("name"));
    }

    @Test
    public void testForIdsIsNullDebugEnabled() {
        String expected =
                "(oid IN (SELECT oid FROM object_property WHERE property_type IN (:"
                        + "ptype0) AND value IS NULL) OR oid NOT IN (SELECT oid FROM object_property WHERE property_type IN (:"
                        + "ptype0))) /* [ name IS NULL ] */\n";
        verifyForIds(expected, true, Predicates.isNull("name"));
    }

    @Test
    public void testForCountIsNotNullDebugDisabled() {
        String expected =
                "NOT (oid IN (SELECT oid FROM object_property WHERE property_type IN (:"
                        + "ptype0) AND value IS NULL) OR oid NOT IN (SELECT oid FROM object_property WHERE property_type IN (:"
                        + "ptype0)))";
        verifyForCount(expected, false, Predicates.not(Predicates.isNull("name")));
    }

    @Test
    public void testForCountIsNotNullDebugEnabled() {
        String expected =
                "NOT (oid IN (SELECT oid FROM object_property WHERE property_type IN (:"
                        + "ptype0) AND value IS NULL) OR oid NOT IN (SELECT oid FROM object_property WHERE property_type IN (:"
                        + "ptype0))) /* [ name IS NULL ] */";
        verifyForCount(expected, true, Predicates.not(Predicates.isNull("name")));
    }

    @Test
    public void testForIdsIsNotNullDebugDisabled() {
        String expected =
                "NOT (oid IN (SELECT oid FROM object_property WHERE property_type IN (:"
                        + "ptype0) AND value IS NULL) OR oid NOT IN (SELECT oid FROM object_property WHERE property_type IN (:"
                        + "ptype0))) ";
        verifyForIds(expected, false, Predicates.not(Predicates.isNull("name")));
    }

    @Test
    public void testForIdsIsNotNullDebugEnabled() {
        String expected =
                "NOT (oid IN (SELECT oid FROM object_property WHERE property_type IN (:"
                        + "ptype0) AND value IS NULL) OR oid NOT IN (SELECT oid FROM object_property WHERE property_type IN (:"
                        + "ptype0))) /* [ name IS NULL ] */\n";
        verifyForIds(expected, true, Predicates.not(Predicates.isNull("name")));
    }

    private void verifyForCount(String expectedSQL, boolean debugMode, Filter filter) {
        String expected = expectedSQL;
        if (!expected.startsWith("SELECT")) {
            expected =
                    "SELECT COUNT(oid) FROM object WHERE type_id IN (:types) "
                            + (debugMode ? "/* org.geoserver.catalog.WorkspaceInfo */\n" : "")
                            + "AND "
                            + expectedSQL;
        }
        QueryBuilder<?> builder = QueryBuilder.forCount(dialect, WorkspaceInfo.class, dbMappings);
        verifyQuery(builder, expected, debugMode, filter);
    }

    private void verifyForIds(
            String expectedSQL, boolean debugMode, Filter filter, SortBy... order) {
        String expected = expectedSQL;
        if (!expected.startsWith("SELECT")) {
            expected =
                    "SELECT id FROM object WHERE type_id IN (:types) "
                            + (debugMode ? "/* org.geoserver.catalog.WorkspaceInfo */\n" : "")
                            + "AND "
                            + expectedSQL
                            + "ORDER BY oid";
        }
        QueryBuilder<?> builder = QueryBuilder.forIds(dialect, WorkspaceInfo.class, dbMappings);
        verifyQuery(builder, expected, debugMode, filter, order);
    }

    private void verifyQuery(
            QueryBuilder<?> builder,
            String expected,
            boolean debugMode,
            Filter filter,
            SortBy... order) {
        dialect.setDebugMode(debugMode);
        String actual = builder.filter(filter).sortOrder(order).build();
        assertEquals(expected, actual);
        if (Filter.INCLUDE.equals(filter)) {
            assertEquals(Filter.INCLUDE, builder.getSupportedFilter());
        } else {
            assertNotEquals(Filter.INCLUDE, builder.getSupportedFilter());
        }
        assertEquals(Filter.INCLUDE, builder.getUnsupportedFilter());
        assertEquals(!actual.startsWith("SELECT COUNT"), builder.isOffsetLimitApplied());
        assertFalse(builder.getNamedParameters().isEmpty());
    }
}
