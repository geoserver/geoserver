/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import static com.google.common.collect.Iterators.toArray;
import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.jayway.jsonpath.DocumentContext;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.geometry.BoundingBox;
import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.FeatureIteratorIterator;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

/**
 * Checks the WGS84 spatial extent in collections from {@code /collections} and {@code /collections/<collection>}
 * documents adhere to the configured number of decimal places, and {@code /collection/<collection>/items} are fully
 * covered by the spatial extent declared in the collection document.
 */
@RunWith(Parameterized.class)
public class CollectionDocumentBoundsTest extends FeaturesTestSupport {

    /** Test dataset with high precision ordinates */
    static QName TIGER_POI = new QName("http://www.census.gov", "poi", "tiger");
    /** Single-point feature dataset with high precision ordinates */
    static QName TIGER_SINGLE_POI = new QName("http://www.census.gov", "single_poi", "tiger");

    static final List<QName> layersFixture = List.of(TIGER_POI, TIGER_SINGLE_POI, CiteTestData.BUILDINGS);

    private String collectionId;
    private int numDecimals;

    /** The actual, accurate lat-lon bbox for the feature type under test */
    private ReferencedEnvelope latLonBbox;

    // Constructor to inject parameters for each test case
    public CollectionDocumentBoundsTest(String collectionId, int numDecimals) {
        this.collectionId = collectionId;
        this.numDecimals = numDecimals;
    }

    // Provide test data [[collectionId, numDecimals],...]
    @Parameters(name = "{0}: numDecimals: {1}")
    public static Collection<Object[]> data() {
        return layersFixture.stream()
                .map(qn -> format("%s:%s", qn.getPrefix(), qn.getLocalPart()))
                .map(CollectionDocumentBoundsTest::testParams)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private static List<Object[]> testParams(String name) {

        return IntStream.rangeClosed(3, 15)
                .boxed()
                .sorted(Comparator.<Integer>naturalOrder().reversed())
                .map(numDecimals -> new Object[] {name, numDecimals})
                .collect(Collectors.toList());
    }

    /**
     * One-time set up, creates the additional layers in the catalog and sets up precise lat-lon bounds for each
     * {@link #layersFixture collection to be tested}
     */
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        addFeatureTypes(testData, catalog);
        layersFixture.stream().map(super::getLayerId).forEach(this::adjustFeatureType);
    }

    /**
     * Obtain the actual collection bounds and store it in {@link #latLonBbox}, configure number of decimal places to
     * use for encoding from the {@link #numDecimals} test parameter
     */
    @Before
    public void beforeEach() throws Exception {
        GeoServer geoServer = getGeoServer();
        GeoServerInfo global = geoServer.getGlobal();
        global.getSettings().setNumDecimals(this.numDecimals);
        geoServer.save(global);

        FeatureTypeInfo featureType = getCatalog().getFeatureTypeByName(collectionId);
        this.latLonBbox = featureType.getLatLonBoundingBox();
    }

    @Test
    public void testCollectionBoundsCoversActualBounds() throws Exception {
        assertEquals(this.numDecimals, getGeoServer().getGlobal().getSettings().getNumDecimals());

        String collectionQuery = format("ogc/features/v1/collections/%s", collectionId);
        DocumentContext json = getAsJSONPath(collectionQuery, 200);

        ReferencedEnvelope actualBounds = this.latLonBbox;
        ReferencedEnvelope encodedBounds = parseSpatialExtent(json);

        assertMaxDecimals(encodedBounds, this.numDecimals);
        assertEncodedCollectionBoundsCoversActualBounds(actualBounds, encodedBounds);
    }

    @Test
    public void testCollectionBoundsCoversItems() throws Exception {
        assertEquals(this.numDecimals, getGeoServer().getGlobal().getSettings().getNumDecimals());

        String collectionQuery = format("ogc/features/v1/collections/%s", collectionId);
        DocumentContext json = getAsJSONPath(collectionQuery, 200);
        final ReferencedEnvelope collectionBounds = parseSpatialExtent(json);

        String itemsQuery = format("ogc/features/v1/collections/%s/items", collectionId);
        String fc = super.getAsString(itemsQuery);
        SimpleFeatureCollection parsedFc = GeoJSONReader.parseFeatureCollection(fc);

        SimpleFeature[] features = toArray(new FeatureIteratorIterator<>(parsedFc.features()), SimpleFeature.class);
        for (SimpleFeature feature : features) {
            assertMaxDecimals(feature, this.numDecimals);
            assertEncodedCollectionBoundsCoversFeature(feature, collectionBounds);
        }
    }

    private void assertMaxDecimals(SimpleFeature feature, int numDecimals) {
        Geometry geom = (Geometry) feature.getDefaultGeometry();
        if (geom == null) {
            assertTrue(true);
        } else {
            String strgeom = geom.toText();
            Coordinate[] coordinates = geom.getCoordinates();
            for (Coordinate c : coordinates) {
                assertMaxDecimals(c.getX(), numDecimals, strgeom);
                assertMaxDecimals(c.getY(), numDecimals, strgeom);
            }
        }
    }

    private void assertMaxDecimals(ReferencedEnvelope bounds, int numDecimals) {
        String strbounds = bounds.toString();
        assertMaxDecimals(bounds.getMinX(), numDecimals, strbounds);
        assertMaxDecimals(bounds.getMaxX(), numDecimals, strbounds);
        assertMaxDecimals(bounds.getMinY(), numDecimals, strbounds);
        assertMaxDecimals(bounds.getMaxY(), numDecimals, strbounds);
    }

    private void assertMaxDecimals(double ordinate, int numDecimals, String message) {
        int numberOfDecimals = getNumberOfDecimals(ordinate);
        assertThat(message, numberOfDecimals, Matchers.lessThanOrEqualTo(numDecimals));
    }

    private void assertEncodedCollectionBoundsCoversActualBounds(
            ReferencedEnvelope original, ReferencedEnvelope encoded) {
        String msg = format(
                "Expected encoded collection bounds %s to fully contain %s%n"
                        + "encoded extent:\t\t%s %noriginal extent:\t%s",
                encoded, original, poly(encoded), poly(original));

        assertTrue(msg, encoded.contains(original));
    }

    private void assertEncodedCollectionBoundsCoversFeature(
            SimpleFeature feature, ReferencedEnvelope encodedCollectionBounds) {

        ReferencedEnvelope actualCollectionBounds = this.latLonBbox;
        String fid = feature.getID();
        BoundingBox featureBounds = feature.getBounds();
        String msg = format(
                "Feature %s bounds not covered by collection bounds%n"
                        + "feature bounds:\t\t%s %ncollection bounds:\t%s%n"
                        + "real collection bounds:\t%s",
                fid, poly(featureBounds), poly(encodedCollectionBounds), poly(actualCollectionBounds));

        assertTrue(msg, encodedCollectionBounds.contains(featureBounds));
    }

    private static Polygon poly(BoundingBox bounds) {
        return JTS.toGeometry(bounds);
    }

    private void addFeatureTypes(SystemTestData testData, Catalog catalog) throws IOException {
        testData.addWorkspace(TIGER_POI.getPrefix(), TIGER_POI.getNamespaceURI(), catalog);
        testData.addVectorLayer(TIGER_POI, null, "tiger_poi.properties", getClass(), catalog);
        testData.addVectorLayer(TIGER_SINGLE_POI, null, "tiger_single_poi.properties", getClass(), catalog);
    }

    private void adjustFeatureType(String featureTypeName) {
        Catalog catalog = getCatalog();
        FeatureTypeInfo featureType = catalog.getFeatureTypeByName(featureTypeName);
        assertNotNull(featureType);
        // revert default values set by SystemTestData.addVectorLayer()
        // sets numDecimals=8 at the featuretype level, revert to zero for the encoder
        // to use the global settings
        featureType.setNumDecimals(0);
        // compute latlon bbox, SystemTestData sets the full WGS84 bounds
        try {
            new CatalogBuilder(catalog).setupBounds(featureType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        ReferencedEnvelope actualLatLonBounds = featureType.getLatLonBoundingBox();
        featureType.setLatLonBoundingBox(actualLatLonBounds);
        catalog.save(featureType);
    }

    public static int getNumberOfDecimals(double value) {
        // Convert the double to BigDecimal with proper precision handling
        BigDecimal bd = BigDecimal.valueOf(value).stripTrailingZeros();
        // Get the scale (number of decimal places)
        return Math.max(0, bd.scale());
    }

    private ReferencedEnvelope parseSpatialExtent(DocumentContext json) {
        double minx = json.read("$.extent.spatial.bbox[0][0]", Double.class);
        double miny = json.read("$.extent.spatial.bbox[0][1]", Double.class);
        double maxx = json.read("$.extent.spatial.bbox[0][2]", Double.class);
        double maxy = json.read("$.extent.spatial.bbox[0][3]", Double.class);

        return new ReferencedEnvelope(minx, maxx, miny, maxy, DefaultGeographicCRS.WGS84);
    }
}
