package org.geoserver.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.appschema.filter.FilterFactoryImplNamespaceAware;
import org.geotools.appschema.jdbc.NestedFilterToSQL;
import org.geotools.data.FeatureSource;
import org.geotools.data.complex.AppSchemaDataAccess;
import org.geotools.data.complex.FeatureTypeMapping;
import org.geotools.data.complex.filter.ComplexFilterSplitter;
import org.geotools.data.jdbc.FilterToSQLException;
import org.geotools.data.util.NullProgressListener;
import org.geotools.feature.NameImpl;
import org.geotools.jdbc.JDBCDataStore;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.Crosses;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Disjoint;
import org.opengis.filter.spatial.Equals;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Overlaps;
import org.opengis.filter.spatial.Touches;
import org.opengis.filter.spatial.Within;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class NestedGeometryFilterEncodingTest extends AbstractAppSchemaTestSupport {

    private static final String GML32_PREFIX = "gml32";

    private static final String STATION_FEATURE = "st_gml32:StationWithMeasurements_gml32";

    private static final Name STATION_FEATURE_NAME =
            new NameImpl(MockData.STATIONS_URI_GML32, "StationWithMeasurements_gml32");

    private static final String STATION_NESTED_GEOM =
            "st_gml32:measurements/ms_gml32:Measurement_gml32/"
                    + "ms_gml32:sampledArea/ms_gml32:SampledArea/ms_gml32:geometry";

    private static final String STATION_NONEXISTENT_NESTED_GEOM =
            "st_gml32:measurements/ms_gml32:Measurement_gml32/"
                    + "ms_gml32:sampledArea/ms_gml32:SampledArea/ms_gml32:not_there_geometry";

    private static final String STATION_WRONG_NESTED_GEOM =
            "st_gml32:measurements/ms_gml32:Measurement_gml32/"
                    + "ms_gml32:sampledArea/ms_gml32:SampledArea/ms_gml32:code";

    AppSchemaDataAccess dataAccess;

    private FeatureTypeMapping rootMapping;

    private FilterFactory2 ff;

    private WKTReader wktReader = new WKTReader();

    @Override
    protected StationsMockData createTestData() {
        // instantiate our custom complex types
        return new MockData();
    }

    private static final class MockData extends StationsMockData {

        @Override
        public void addContent() {
            // add GML 3.2 namespaces
            putNamespace(STATIONS_PREFIX_GML32, STATIONS_URI_GML32);
            putNamespace(MEASUREMENTS_PREFIX_GML32, MEASUREMENTS_URI_GML32);
            // add GML 3.2 feature types
            Map<String, String> gml32Parameters = new HashMap<>();
            gml32Parameters.put("GML_PREFIX", GML32_PREFIX);
            gml32Parameters.put("GML_NAMESPACE", "http://www.opengis.net/gml/3.2");
            gml32Parameters.put("GML_LOCATION", "http://schemas.opengis.net/gml/3.2.1/gml.xsd");
            addStationFeatureType(
                    STATIONS_PREFIX_GML32,
                    GML32_PREFIX,
                    "StationWithMeasurements",
                    "stations",
                    "defaultGeometry/stations2.xml",
                    "measurements",
                    "defaultGeometry/measurements.xml",
                    gml32Parameters);
            addMeasurementFeatureType(
                    MEASUREMENTS_PREFIX_GML32,
                    GML32_PREFIX,
                    "measurements",
                    "defaultGeometry/measurements.xml",
                    gml32Parameters);
        }

        /**
         * Helper method that will add the station feature type customizing it for the desired GML
         * version.
         */
        protected void addStationFeatureType(
                String namespacePrefix,
                String gmlPrefix,
                String stationsFeatureType,
                String stationsMappingsName,
                String stationsMappingsPath,
                String measurementsMappingsName,
                String measurementsMappingsPath,
                Map<String, String> parameters) {
            // create root directory
            File gmlDirectory = getDirectoryForGmlPrefix(gmlPrefix);
            gmlDirectory.mkdirs();
            // add the necessary files
            File stationsMappings =
                    new File(
                            gmlDirectory,
                            String.format("%s_%s.xml", stationsMappingsName, gmlPrefix));
            File stationsProperties =
                    new File(gmlDirectory, String.format("stations_%s.properties", gmlPrefix));
            File stationsSchema =
                    new File(gmlDirectory, String.format("stations_%s.xsd", gmlPrefix));
            File measurementsSchema =
                    new File(gmlDirectory, String.format("measurements_%s.xsd", gmlPrefix));
            // perform the parameterization
            substituteParameters(
                    "/test-data/stations/" + stationsMappingsPath, parameters, stationsMappings);
            substituteParameters(
                    "/test-data/stations/defaultGeometry/stations.properties",
                    parameters,
                    stationsProperties);
            substituteParameters(
                    "/test-data/stations/defaultGeometry/stations.xsd", parameters, stationsSchema);
            substituteParameters(
                    "/test-data/stations/defaultGeometry/measurements.xsd",
                    parameters,
                    measurementsSchema);
            // create station feature type
            addFeatureType(
                    namespacePrefix,
                    String.format("%s_%s", stationsFeatureType, gmlPrefix),
                    stationsMappings.getAbsolutePath(),
                    stationsProperties.getAbsolutePath(),
                    stationsSchema.getAbsolutePath(),
                    measurementsSchema.getAbsolutePath());
        }

        @Override
        protected void addMeasurementFeatureType(
                String namespacePrefix,
                String gmlPrefix,
                String mappingsName,
                String mappingsPath,
                Map<String, String> parameters) {
            // create root directory
            File gmlDirectory = getDirectoryForGmlPrefix(gmlPrefix);
            gmlDirectory.mkdirs();
            // add the necessary files
            File measurementsMappings =
                    new File(gmlDirectory, String.format("%s_%s.xml", mappingsName, gmlPrefix));
            File measurementsProperties =
                    new File(gmlDirectory, String.format("measurements_%s.properties", gmlPrefix));
            File measurementsSchema =
                    new File(gmlDirectory, String.format("measurements_%s.xsd", gmlPrefix));
            // perform the parameterization
            substituteParameters(
                    "/test-data/stations/" + mappingsPath, parameters, measurementsMappings);
            substituteParameters(
                    "/test-data/stations/defaultGeometry/measurements.properties",
                    parameters,
                    measurementsProperties);
            substituteParameters(
                    "/test-data/stations/defaultGeometry/measurements.xsd",
                    parameters,
                    measurementsSchema);
            // create measurements feature type
            addFeatureType(
                    namespacePrefix,
                    String.format("Measurement_%s", gmlPrefix),
                    measurementsMappings.getAbsolutePath(),
                    measurementsProperties.getAbsolutePath(),
                    measurementsSchema.getAbsolutePath());
        }
    }

    @Before
    public void setUpTest() throws IOException {
        FeatureTypeInfo ftInfo = getCatalog().getFeatureTypeByName(STATION_FEATURE);
        FeatureSource fs = ftInfo.getFeatureSource(new NullProgressListener(), null);

        dataAccess = (AppSchemaDataAccess) fs.getDataStore();
        rootMapping = dataAccess.getMappingByNameOrElement(ftInfo.getQualifiedName());
        assertNotNull(rootMapping);

        ff = new FilterFactoryImplNamespaceAware(rootMapping.getNamespaces());
    }

    @Test
    public void testNestedBBOXFilterEncoding() throws FilterToSQLException, IOException {
        // make sure nested filters encoding is enabled, otherwise skip test
        assumeTrue(shouldTestNestedFiltersEncoding(rootMapping));

        PropertyName nestedGeom = ff.property(STATION_NESTED_GEOM);
        BBOX bbox = ff.bbox(nestedGeom, -4, 2.5, 0, 4, "EPSG:4326");

        checkPostPreFilterSplitting(bbox);

        checkFilterEncoding(bbox);

        checkFeatures(bbox, "2");
    }

    @Test
    public void testNestedContainsFilterEncoding()
            throws FilterToSQLException, IOException, ParseException {
        // make sure nested filters encoding is enabled, otherwise skip test
        assumeTrue(shouldTestNestedFiltersEncoding(rootMapping));

        PropertyName nestedGeom = ff.property(STATION_NESTED_GEOM);
        Polygon contained =
                (Polygon)
                        wktReader.read("POLYGON((-1.5 -1.5, -1.5 1.5, 0 1.5, 0 -1.5, -1.5 -1.5))");
        Contains contains = ff.contains(nestedGeom, ff.literal(contained));

        checkPostPreFilterSplitting(contains);

        checkFilterEncoding(contains);

        checkFeatures(contains, "1");
    }

    @Test
    public void testNestedTouchesFilterEncoding()
            throws FilterToSQLException, IOException, ParseException {
        // make sure nested filters encoding is enabled, otherwise skip test
        assumeTrue(shouldTestNestedFiltersEncoding(rootMapping));

        PropertyName nestedGeom = ff.property(STATION_NESTED_GEOM);
        Polygon touching = (Polygon) wktReader.read("POLYGON((-5 -2, -5 0, -4 0, -4 -2, -5 -2))");
        Touches touches = ff.touches(nestedGeom, ff.literal(touching));

        checkPostPreFilterSplitting(touches);

        checkFilterEncoding(touches);

        checkFeatures(touches, "3");
    }

    @Test
    public void testNestedIntersectsFilterEncoding()
            throws FilterToSQLException, IOException, ParseException {
        // make sure nested filters encoding is enabled, otherwise skip test
        assumeTrue(shouldTestNestedFiltersEncoding(rootMapping));

        PropertyName nestedGeom = ff.property(STATION_NESTED_GEOM);
        Polygon intersecting = (Polygon) wktReader.read("POLYGON((0 0, -4 4, 0 4, 0 0))");
        Intersects intersects = ff.intersects(nestedGeom, ff.literal(intersecting));

        checkPostPreFilterSplitting(intersects);

        checkFilterEncoding(intersects);

        checkFeatures(intersects, "1", "2");
    }

    @Test
    public void testNestedOverlapsFilterEncoding()
            throws FilterToSQLException, IOException, ParseException {
        // make sure nested filters encoding is enabled, otherwise skip test
        assumeTrue(shouldTestNestedFiltersEncoding(rootMapping));

        PropertyName nestedGeom = ff.property(STATION_NESTED_GEOM);
        Polygon intersecting =
                (Polygon) wktReader.read("POLYGON((-4 3, -2 4.5, -2.5 2, -3.5 2, -4 3))");
        Overlaps overlaps = ff.overlaps(nestedGeom, ff.literal(intersecting));

        checkPostPreFilterSplitting(overlaps);

        checkFilterEncoding(overlaps);

        checkFeatures(overlaps, "2");
    }

    @Test
    public void testNestedWithinFilterEncoding() throws Exception {
        // make sure nested filters encoding is enabled, otherwise skip test
        assumeTrue(shouldTestNestedFiltersEncoding(rootMapping));

        PropertyName nestedGeom = ff.property(STATION_NESTED_GEOM);
        Polygon container = (Polygon) wktReader.read("POLYGON((-4 -3, -4 3, -1 3, -1 -3, -4 -3))");
        Within within = ff.within(nestedGeom, ff.literal(container));

        checkPostPreFilterSplitting(within);

        checkFilterEncoding(within);

        checkFeatures(within, "2", "3");
    }

    @Test
    public void testNestedCrossesFilterEncoding() throws Exception {
        // make sure nested filters encoding is enabled, otherwise skip test
        assumeTrue(shouldTestNestedFiltersEncoding(rootMapping));

        PropertyName nestedGeom = ff.property(STATION_NESTED_GEOM);
        LineString container = (LineString) wktReader.read("LINESTRING(-5.5 -5, -5 -3, -3 -2)");
        Crosses crosses = ff.crosses(ff.literal(container), nestedGeom);

        checkPostPreFilterSplitting(crosses);

        checkFilterEncoding(crosses);

        checkFeatures(crosses, "3");
    }

    @Test
    public void testNestedDisjointFilterEncoding() throws Exception {
        // make sure nested filters encoding is enabled, otherwise skip test
        assumeTrue(shouldTestNestedFiltersEncoding(rootMapping));

        PropertyName nestedGeom = ff.property(STATION_NESTED_GEOM);
        Polygon poly = (Polygon) wktReader.read("POLYGON((-5 -4, -5 -2, -3 -2, -3 -4, -5 -4))");
        Disjoint disjoint = ff.disjoint(ff.literal(poly), nestedGeom);

        checkPostPreFilterSplitting(disjoint);

        checkFilterEncoding(disjoint);

        checkFeatures(disjoint, "1", "2");
    }

    @Test
    public void testNestedEqualsFilterEncoding() throws Exception {
        // make sure nested filters encoding is enabled, otherwise skip test
        assumeTrue(shouldTestNestedFiltersEncoding(rootMapping));

        PropertyName nestedGeom = ff.property(STATION_NESTED_GEOM);
        Polygon st1 = (Polygon) wktReader.read("POLYGON((-2 2, 0 2, 0 -2, -2 -2, -2 2))");
        Equals equal = ff.equal(ff.literal(st1), nestedGeom);

        checkPostPreFilterSplitting(equal);

        checkFilterEncoding(equal);

        checkFeatures(equal, "1");
    }

    @Test
    public void testNestedBeyondFilterEncoding() throws Exception {
        // make sure nested filters encoding is enabled, otherwise skip test
        assumeTrue(shouldTestNestedFiltersEncoding(rootMapping));

        CoordinateReferenceSystem crs = getCoordinateReferenceSystem();
        String units = crs.getCoordinateSystem().getAxis(0).getUnit().toString();

        PropertyName nestedGeom = ff.property(STATION_NESTED_GEOM);
        Polygon beyondSt2 =
                (Polygon) wktReader.read("POLYGON((-5 -4, -5 -2.5, -2 -2.5, -2 -4, -5 -4))");
        Beyond beyond = ff.beyond(nestedGeom, ff.literal(beyondSt2), 1.0, units);

        checkPostPreFilterSplitting(beyond);
        checkFilterEncoding(beyond);
        checkFeatures(beyond, "1", "2");
    }

    @Test
    public void testNestedDWithinFilterEncoding() throws Exception {
        // make sure nested filters encoding is enabled, otherwise skip test
        assumeTrue(shouldTestNestedFiltersEncoding(rootMapping));

        CoordinateReferenceSystem crs = getCoordinateReferenceSystem();
        String units = crs.getCoordinateSystem().getAxis(0).getUnit().toString();

        PropertyName nestedGeom = ff.property(STATION_NESTED_GEOM);
        Polygon dwithinSt2 =
                (Polygon) wktReader.read("POLYGON((-5 -4, -5 -2.5, -2 -2.5, -2 -4, -5 -4))");
        DWithin dwithin = ff.dwithin(nestedGeom, ff.literal(dwithinSt2), 1.0, units);

        checkPostPreFilterSplitting(dwithin);

        checkFilterEncoding(dwithin);

        checkFeatures(dwithin, "3");
    }

    @Test
    public void testNesteGeometryFilterOnNonExistentProperty() throws Exception {
        // make sure nested filters encoding is enabled, otherwise skip test
        assumeTrue(shouldTestNestedFiltersEncoding(rootMapping));

        PropertyName nestedGeom = ff.property(STATION_NONEXISTENT_NESTED_GEOM);
        Polygon intersecting = (Polygon) wktReader.read("POLYGON((0 0, -4 4, 0 4, 0 0))");
        Intersects intersects = ff.intersects(nestedGeom, ff.literal(intersecting));

        try {
            checkPostPreFilterSplitting(intersects);
            fail("Expected IllegalArgumentException to be thrown, but none was thrown instead");
        } catch (IllegalArgumentException iae) {
            String errorMessage = iae.getMessage();
            assertTrue(errorMessage.contains("not_there_geometry"));
            assertTrue(errorMessage.contains("not found in type"));
        } catch (Exception other) {
            fail(
                    "Expected IllegalArgumentException to be thrown, but "
                            + other.getClass().getName()
                            + " was thrown instead");
        }
    }

    @Test
    public void testNesteGeometryFilterOnNonGeometryProperty() throws Exception {
        // make sure nested filters encoding is enabled, otherwise skip test
        assumeTrue(shouldTestNestedFiltersEncoding(rootMapping));

        PropertyName nestedGeom = ff.property(STATION_WRONG_NESTED_GEOM);
        BBOX bbox = ff.bbox(nestedGeom, -4, 2.5, 0, 4, "EPSG:4326");

        try {
            checkPostPreFilterSplitting(bbox);
            fail("Expected IllegalArgumentException to be thrown, but none was thrown instead");
        } catch (IllegalArgumentException iae) {
            String errorMessage = iae.getMessage();
            assertTrue(errorMessage.contains("code"));
            assertTrue(errorMessage.contains("AttributeDescriptor"));
            assertTrue(errorMessage.contains("should be of type"));
            assertTrue(errorMessage.contains("GeometryDescriptor"));
        } catch (Exception other) {
            fail(
                    "Expected IllegalArgumentException to be thrown, but "
                            + other.getClass().getName()
                            + " was thrown instead");
        }
    }

    CoordinateReferenceSystem getCoordinateReferenceSystem() throws IOException {
        return dataAccess
                .getSchema(STATION_FEATURE_NAME)
                .getGeometryDescriptor()
                .getCoordinateReferenceSystem();
    }

    private void checkPostPreFilterSplitting(Filter filter) {
        JDBCDataStore store = (JDBCDataStore) rootMapping.getSource().getDataStore();
        ComplexFilterSplitter splitter =
                new ComplexFilterSplitter(store.getFilterCapabilities(), rootMapping);
        filter.accept(splitter, null);
        Filter preFilter = splitter.getFilterPre();
        Filter postFilter = splitter.getFilterPost();

        assertEquals(filter, preFilter);
        assertEquals(Filter.INCLUDE, postFilter);
    }

    private void checkFilterEncoding(Filter filter) throws FilterToSQLException {
        // filter must be "unrolled" (i.e. reverse mapped) first
        Filter unrolled = AppSchemaDataAccess.unrollFilter(filter, rootMapping);

        // Filter is nested
        assertTrue(NestedFilterToSQL.isNestedFilter(unrolled));

        // Encode nested filter
        NestedFilterToSQL nestedFilterToSQL = createNestedFilterEncoder(rootMapping);
        String encodedFilter = nestedFilterToSQL.encodeToString(unrolled);

        assertTrue(encodedFilter.contains("EXISTS"));
    }

    private void checkFeatures(Filter filter, String... fids) throws IOException {
        FeatureSource fs = dataAccess.getFeatureSource(STATION_FEATURE_NAME);
        assertContainsFeatures(fs.getFeatures(filter), fids);
    }
}
