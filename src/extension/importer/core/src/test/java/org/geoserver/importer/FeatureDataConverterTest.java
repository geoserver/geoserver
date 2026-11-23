/* (c) 2013 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import static org.geotools.jdbc.JDBCDataStore.JDBC_NATIVE_SRID;
import static org.geotools.jdbc.JDBCDataStore.JDBC_NATIVE_TYPE;
import static org.geotools.jdbc.JDBCDataStore.JDBC_NATIVE_TYPENAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.util.URLs;
import org.junit.Test;
import org.locationtech.jts.geom.Polygon;

/** @author Ian Schneider <ischneider@opengeo.org> */
public class FeatureDataConverterTest {

    private static final String POSTGIS = "postgis";

    @Test
    public void testXMLUnsafeAttributeRenaming() {
        SimpleFeatureType badatts = buildFeatureTypeWithXMLUnsafeAtts();
        badatts = FeatureDataConverter.DEFAULT.convertType(badatts, null, null, null);

        assertEquals(
                "_123_number_first", badatts.getAttributeDescriptors().get(0).getLocalName());
        assertEquals("i_has_spaces", badatts.getAttributeDescriptors().get(1).getLocalName());
    }

    @Test
    public void testPostgisConversion() {
        SimpleFeatureType t =
                FeatureDataConverter.TO_POSTGIS.convertType(buildFeatureTypeWithXMLUnsafeAtts(), null, null, null);
        assertEquals("_123_number_first", t.getAttributeDescriptors().get(0).getLocalName());
        assertEquals("i_has_spaces", t.getAttributeDescriptors().get(1).getLocalName());
    }

    @Test
    public void testOracleConversion() {
        SimpleFeatureType t =
                FeatureDataConverter.TO_ORACLE.convertType(buildFeatureTypeWithXMLUnsafeAtts(), null, null, null);
        assertEquals("_123_NUMBER_FIRST", t.getAttributeDescriptors().get(0).getLocalName());
        assertEquals("I_HAS_SPACES", t.getAttributeDescriptors().get(1).getLocalName());
    }

    /** A GeoPackage file imported into PostGIS gets PostGIS types, not the plain PostgreSQL POLYGON. */
    @Test
    public void testGeoPackageFileToPostGIS() throws Exception {
        SimpleFeatureType source = cookbookPolygonsGpkg();
        assertEquals("POLYGON", source.getDescriptor("geom").getUserData().get(JDBC_NATIVE_TYPENAME));
        assertEquals("TEXT", source.getDescriptor("name").getUserData().get(JDBC_NATIVE_TYPENAME));

        // a file has no source table
        SimpleFeatureType converted = typeToPostGIS(source, null);
        assertNoNativeTypes(converted);
        GeometryDescriptor geometry = converted.getGeometryDescriptor();
        assertEquals(Polygon.class, geometry.getType().getBinding());
        assertEquals(source.getCoordinateReferenceSystem(), geometry.getCoordinateReferenceSystem());

        // the source store keeps its types
        assertEquals("POLYGON", source.getDescriptor("geom").getUserData().get(JDBC_NATIVE_TYPENAME));
    }

    /** A table of another kind of database loses its native types too. */
    @Test
    public void testGeoPackageTableToPostGIS() throws Exception {
        SimpleFeatureType converted = typeToPostGIS(cookbookPolygonsGpkg(), table("geopkg"));
        assertNoNativeTypes(converted);
    }

    /**
     * A table of the same kind of database keeps its native types, such as arrays, uuid or geography in PostGIS. The
     * converter compares the kinds of store only, so a GeoPackage type stands in for a PostGIS one.
     */
    @Test
    public void testPostGISTableToPostGIS() throws Exception {
        SimpleFeatureType source = cookbookPolygonsGpkg();
        SimpleFeatureType converted = typeToPostGIS(source, table(POSTGIS));
        assertEquals("POLYGON", converted.getDescriptor("geom").getUserData().get(JDBC_NATIVE_TYPENAME));
        for (AttributeDescriptor descriptor : source.getAttributeDescriptors()) {
            String name = descriptor.getLocalName();
            Map<Object, Object> expected = descriptor.getUserData();
            Map<Object, Object> actual = converted.getDescriptor(name).getUserData();
            assertEquals(expected.get(JDBC_NATIVE_TYPENAME), actual.get(JDBC_NATIVE_TYPENAME));
            assertEquals(expected.get(JDBC_NATIVE_TYPE), actual.get(JDBC_NATIVE_TYPE));
            assertEquals(expected.get(JDBC_NATIVE_SRID), actual.get(JDBC_NATIVE_SRID));
        }
    }

    private static void assertNoNativeTypes(SimpleFeatureType type) {
        for (AttributeDescriptor descriptor : type.getAttributeDescriptors()) {
            assertNull(descriptor.getUserData().get(JDBC_NATIVE_TYPENAME));
            assertNull(descriptor.getUserData().get(JDBC_NATIVE_TYPE));
            assertNull(descriptor.getUserData().get(JDBC_NATIVE_SRID));
        }
    }

    /** Reads the type of the polygon layer of the cookbook GeoPackage. */
    private SimpleFeatureType cookbookPolygonsGpkg() throws IOException {
        URL cookbook = getClass().getResource("test-data/gpkg/cookbook.gpkg");
        String path = URLs.urlToFile(cookbook).getPath();
        Map<String, Object> params = Map.of("dbtype", "geopkg", "database", path);
        DataStore store = DataStoreFinder.getDataStore(params);
        try {
            return store.getSchema("polygon");
        } finally {
            store.dispose();
        }
    }

    /** Returns a table of a database of the given kind, as a database import reads it. */
    private static Table table(String dbtype) {
        return new Table("polygon", new Database(Map.of(JDBCDataStoreFactory.DBTYPE.key, dbtype)));
    }

    /** Converts the type for a PostGIS store, as the importer does before creating the table. */
    private static SimpleFeatureType typeToPostGIS(SimpleFeatureType source, ImportData data) {
        ImportTask task = new ImportTask();
        LayerInfo layer = new LayerInfoImpl();
        layer.setResource(new FeatureTypeInfoImpl(null));
        layer.setName("polygon");
        task.setLayer(layer);
        DataStoreInfoImpl store = new DataStoreInfoImpl(null);
        store.getConnectionParameters().put(JDBCDataStoreFactory.DBTYPE.key, POSTGIS);
        task.setStore(store);
        return FeatureDataConverter.TO_POSTGIS.convertType(source, null, data, task);
    }

    SimpleFeatureType buildFeatureTypeWithXMLUnsafeAtts() {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("badatts");

        AttributeTypeBuilder attBuilder = new AttributeTypeBuilder();
        attBuilder.setBinding(String.class);
        typeBuilder.add(attBuilder.buildDescriptor("123_number_first"));
        attBuilder.setBinding(String.class);
        typeBuilder.add(attBuilder.buildDescriptor("i has spaces"));

        return typeBuilder.buildFeatureType();
    }

    @Test
    public void testLayerNameFromTaskLabel() {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("badname");
        SimpleFeatureType schema = typeBuilder.buildFeatureType();

        ImportTask task = new ImportTask();
        LayerInfo layer = new LayerInfoImpl();
        ResourceInfo resource = new FeatureTypeInfoImpl(null);
        layer.setResource(resource);
        layer.setName("goodname");
        task.setLayer(layer);

        schema = FeatureDataConverter.DEFAULT.convertType(schema, null, null, task);

        assertEquals("goodname", schema.getName().getLocalPart());
    }

    @Test
    public void testLayerNameFromTaskNative() {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("badname");
        SimpleFeatureType schema = typeBuilder.buildFeatureType();

        ImportTask task = new ImportTask();
        LayerInfo layer = new LayerInfoImpl();
        ResourceInfo resource = new FeatureTypeInfoImpl(null);
        resource.setNativeName("tablename");
        layer.setResource(resource);
        layer.setName("goodname");
        task.setLayer(layer);

        schema = FeatureDataConverter.DEFAULT.convertType(schema, null, null, task);

        assertEquals("tablename", schema.getName().getLocalPart());
    }
}
