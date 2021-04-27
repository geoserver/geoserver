package org.geoserver.schemalessfeatures.mongodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geotools.feature.NameImpl;
import org.geotools.image.test.ImageAssert;
import org.geotools.util.URLs;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

@SuppressWarnings({
    "PMD.JUnit4TestShouldUseAfterAnnotation",
    "PMD.JUnit4TestShouldUseBeforeAnnotation"
})
public class WMSSchemalessMongoTest extends AbstractMongoDBOnlineTestSupport {

    private static final String DATA_STORE_NAME = "stationsMongoWfs";

    private static MongoTestSetup testSetup;

    private static final String STYLE_ST = "stations";

    private static final String STYLE_ST_RT = "stationsRT";

    private static final String STYLE_ST_SORT_ASC = "stationsSortAsc";

    private static final String STYLE_ST_SORT_DESC = "stationsSortDesc";

    private static final String STYLE_NULLABLE_FIELD = "stationsFilterOnNullableField";

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        Catalog cat = getCatalog();
        DataStoreInfo storeInfo = cat.getDataStoreByName(DATA_STORE_NAME);
        if (storeInfo == null) {
            WorkspaceInfo wi = cat.getDefaultWorkspace();
            storeInfo = addMongoSchemalessStore(wi, DATA_STORE_NAME);
            addMongoSchemalessLayer(wi, storeInfo, StationsTestSetup.COLLECTION_NAME);
            LayerInfo li =
                    cat.getLayerByName(
                            new NameImpl(wi.getName(), StationsTestSetup.COLLECTION_NAME));
            StyleInfo st = cat.getStyleByName(STYLE_ST);
            li.setDefaultStyle(st);
            StyleInfo stRT = cat.getStyleByName(STYLE_ST_RT);
            li.getStyles().add(stRT);
            StyleInfo stSortAsc = cat.getStyleByName(STYLE_ST_SORT_ASC);
            li.getStyles().add(stSortAsc);
            StyleInfo stSortDesc = cat.getStyleByName(STYLE_ST_SORT_DESC);
            li.getStyles().add(stSortDesc);
            StyleInfo stNullableField = cat.getStyleByName(STYLE_NULLABLE_FIELD);
            li.getStyles().add(stNullableField);
            cat.save(li);
        }
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        testData.addStyle(
                STYLE_ST, "./test-data/stations/styles/stations.sld", getClass(), catalog);
        testData.addStyle(
                STYLE_ST_RT,
                "./test-data/stations/styles/stations_with_RT.sld",
                getClass(),
                catalog);
        testData.addStyle(
                STYLE_ST_SORT_ASC,
                "./test-data/stations/styles/stations_with_sort_by_asc.sld",
                getClass(),
                catalog);
        testData.addStyle(
                STYLE_ST_SORT_DESC,
                "./test-data/stations/styles/stations_with_sort_by_desc.sld",
                getClass(),
                catalog);

        testData.addStyle(
                STYLE_NULLABLE_FIELD,
                "./test-data/stations/styles/stationsFilterOnNullableField.sld",
                getClass(),
                catalog);
    }

    @Override
    protected MongoTestSetup createTestSetups() {
        testSetup = new StationsTestSetup(databaseName);
        return testSetup;
    }

    @AfterClass
    public static void tearDown() {
        if (testSetup != null) testSetup.tearDown();
    }

    @Test
    public void testStationsWmsGetMap() throws Exception {
        // execute the WMS GetMap request
        MockHttpServletResponse result =
                getAsServletResponse(
                        "wms?SERVICE=WMS&VERSION=1.1.1"
                                + "&REQUEST=GetMap&FORMAT=image/png&TRANSPARENT=true&STYLES&LAYERS=gs:"
                                + StationsTestSetup.COLLECTION_NAME
                                + "&SRS=EPSG:4326&WIDTH=349&HEIGHT=768"
                                + "&BBOX=96.251220703125,-57.81005859375,103.919677734375,-40.93505859375");
        assertEquals(result.getStatus(), 200);
        assertEquals(result.getContentType(), "image/png");
        // check that we got the expected image back
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(getBinary(result)));
        ImageAssert.assertEquals(
                URLs.urlToFile(getClass().getResource("wms-results/stations-style-result.png")),
                image,
                240);
    }

    @Test
    public void testStationsWmsGetMapWithSortByDesc() throws Exception {

        // execute the WMS GetMap request
        MockHttpServletResponse result =
                getAsServletResponse(
                        "wms?SERVICE=WMS&VERSION=1.1.1"
                                + "&REQUEST=GetMap&FORMAT=image/png&TRANSPARENT=true&STYLES="
                                + STYLE_ST_SORT_DESC
                                + "&LAYERS=gs:"
                                + StationsTestSetup.COLLECTION_NAME
                                + "&SRS=EPSG:4326&WIDTH=349&HEIGHT=768"
                                + "&BBOX=96.251220703125,-57.81005859375,103.919677734375,-40.93505859375");
        assertEquals(result.getStatus(), 200);
        assertEquals(result.getContentType(), "image/png");
        // check that we got the expected image back
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(getBinary(result)));
        // use same image as test asc to check for equality since in the desc sld
        // the two symbolizers have been shifted with respect to the asc sld
        ImageAssert.assertEquals(
                URLs.urlToFile(getClass().getResource("wms-results/stations-style-sort.png")),
                image,
                240);
    }

    @Test
    public void testStationsWmsGetMapWithSortByAsc() throws Exception {
        // execute the WMS GetMap request
        MockHttpServletResponse result =
                getAsServletResponse(
                        "wms?SERVICE=WMS&VERSION=1.1.1"
                                + "&REQUEST=GetMap&FORMAT=image/png&TRANSPARENT=true&STYLES="
                                + STYLE_ST_SORT_ASC
                                + "&LAYERS=gs:"
                                + StationsTestSetup.COLLECTION_NAME
                                + "&SRS=EPSG:4326&WIDTH=349&HEIGHT=768"
                                + "&BBOX=96.251220703125,-57.81005859375,103.919677734375,-40.93505859375");
        assertEquals(result.getStatus(), 200);
        assertEquals(result.getContentType(), "image/png");
        // check that we got the expected image back
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(getBinary(result)));
        // use same image as test desc to check for equality since in the asc sld
        // the two symbolizers have been shifted with respect to the desc sld
        ImageAssert.assertEquals(
                URLs.urlToFile(getClass().getResource("wms-results/stations-style-sort.png")),
                image,
                240);
    }

    @Test
    public void testStationsWmsGetMapWithRendering() throws Exception {
        // execute the WMS GetMap request
        MockHttpServletResponse result =
                getAsServletResponse(
                        "wms?SERVICE=WMS&VERSION=1.1.1"
                                + "&REQUEST=GetMap&FORMAT=image/png&TRANSPARENT=true&STYLES="
                                + STYLE_ST_RT
                                + "&LAYERS=gs:"
                                + StationsTestSetup.COLLECTION_NAME
                                + "&SRS=EPSG:4326&WIDTH=576&HEIGHT=768"
                                + "&BBOX=5,-60,110,80");
        assertEquals(result.getStatus(), 200);
        assertEquals(result.getContentType(), "image/png");
        // check that we got the expected image back
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(getBinary(result)));

        ImageAssert.assertEquals(
                URLs.urlToFile(getClass().getResource("wms-results/stations-rt.png")), image, 240);
    }

    @Test
    public void testStationsWmsGetFeatureInfo() throws Exception {
        // execute the WMS GetFeatureInfo request
        JSONObject json =
                (JSONObject)
                        getAsJSON(
                                "wms?SERVICE=WMS&VERSION=1.1.1"
                                        + "&REQUEST=GetFeatureInfo&FORMAT=image/png&TRANSPARENT=true&QUERY_LAYERS=gs:"
                                        + StationsTestSetup.COLLECTION_NAME
                                        + "&STYLES&LAYERS=gs:"
                                        + StationsTestSetup.COLLECTION_NAME
                                        + "&INFO_FORMAT=application/json"
                                        + "&FEATURE_COUNT=50&X=50&Y=50&SRS=EPSG:4326&WIDTH=101&HEIGHT=101"
                                        + "&BBOX=91.23046875,-58.623046874999986,108.984375,-40.869140624999986");
        JSONArray features = json.getJSONArray("features");
        assertEquals(3, features.size());
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = features.getJSONObject(i);
            checkStationFeature(feature);
        }
    }

    @Test
    public void testStationsWmsGetMapStyleNullableField() throws Exception {
        // execute the WMS GetMap request
        MockHttpServletResponse result =
                getAsServletResponse(
                        "wms?SERVICE=WMS&VERSION=1.1.1"
                                + "&REQUEST=GetMap&FORMAT=image/png&TRANSPARENT=true&STYLES="
                                + STYLE_NULLABLE_FIELD
                                + "&LAYERS=gs:"
                                + StationsTestSetup.COLLECTION_NAME
                                + "&SRS=EPSG:4326&WIDTH=349&HEIGHT=768"
                                + "&BBOX=96.251220703125,-57.81005859375,103.919677734375,-40.93505859375");
        assertEquals(result.getStatus(), 200);
        assertEquals(result.getContentType(), "image/png");
        // check that we got the expected image back
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(getBinary(result)));
        ImageAssert.assertEquals(
                URLs.urlToFile(getClass().getResource("wms-results/stations-nullable-result.png")),
                image,
                240);
    }

    private void checkStationFeature(JSONObject station) {
        JSONObject properties = station.getJSONObject("properties");
        JSONObject geometry = station.getJSONObject("geometry");
        assertNotNull(geometry);
        assertTrue(geometry.has("type"));
        assertTrue(geometry.has("coordinates"));
        assertNotNull(properties.get("id"));
        assertNotNull(properties.get("name"));
        assertNotNull(properties.get("numericValue"));
        JSONObject contact = properties.getJSONObject("contact");
        assertNotNull(contact);
        assertEquals(1, contact.size());
        JSONArray measurements = properties.getJSONArray("measurements");
        assertNotNull(measurements);
        assertTrue(measurements.size() > 0);
    }
}
