/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.jayway.jsonpath.DocumentContext;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.CQL2Conformance;
import org.geoserver.ogcapi.ECQLConformance;
import org.geoserver.ogcapi.OGCApiTestSupport;
import org.geoserver.wms.WMSInfo;
import org.junit.BeforeClass;
import org.springframework.mock.web.MockHttpServletResponse;

public class MapsTestSupport extends OGCApiTestSupport {
    protected static final QName TIMESERIES = new QName(MockData.SF_URI, "timeseries", MockData.SF_PREFIX);
    static final QName TIME_WITH_START_END = new QName(MockData.SF_URI, "TimeWithStartEnd", MockData.SF_PREFIX);

    /** An image mosaic, whose structured reader can apply a filter on the granule index. */
    static final QName WATER_TEMP = new QName(MockData.SF_URI, "watertemp", MockData.SF_PREFIX);

    static final QName TIME_WITH_START_END_DATE =
            new QName(MockData.SF_URI, "TimeWithStartEndDate", MockData.SF_PREFIX);

    @BeforeClass
    public static void setupTimeZone() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpDefault();
        testData.setUpDefaultRasterLayers();
    }

    /** Adds {@link #WATER_TEMP}, left out of the default setup because it would change the collection counts. */
    protected void addWaterTemp(SystemTestData testData) throws Exception {
        testData.addRasterLayer(WATER_TEMP, "watertemp.zip", null, null, SystemTestData.class, getCatalog());
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // add a red style and set it as alternative style for lakes
        Catalog catalog = getCatalog();
        testData.addStyle("red", getClass(), catalog);
        StyleInfo redStyle = catalog.getStyleByName("red");
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        lakes.getStyles().add(redStyle);
        catalog.save(lakes);

        // setup the bbox for lakes
        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setupBounds(lakes.getResource());
        catalog.save(lakes.getResource());

        // add temporal layer
        testData.addRasterLayer(TIMESERIES, "timeseries.zip", null, catalog);
        testData.addVectorLayer(
                TIME_WITH_START_END,
                Collections.emptyMap(),
                "TimeElevationWithStartEnd.properties",
                getClass(),
                catalog);
        testData.addVectorLayer(
                TIME_WITH_START_END_DATE,
                Collections.emptyMap(),
                "TimeElevationWithStartEndDate.properties",
                getClass(),
                catalog);
    }

    /** A test body that may throw, used by {@link #withConformance}. */
    @FunctionalInterface
    protected interface ThrowingRunnable {
        void run() throws Exception;
    }

    /** Flips one Maps conformance flag, runs the body, and always resets the flag to its default (null) afterwards. */
    protected void withConformance(BiConsumer<MapsConformance, Boolean> flag, boolean value, ThrowingRunnable body)
            throws Exception {
        GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        flag.accept(MapsConformance.configuration(wms), value);
        gs.save(wms);
        try {
            body.run();
        } finally {
            flag.accept(MapsConformance.configuration(wms), null);
            gs.save(wms);
        }
    }

    /**
     * Applies a change to the WMS service configuration, runs the body, and always restores the stock configuration
     * afterwards, whatever the body did.
     */
    protected void withWms(Consumer<WMSInfo> mutation, ThrowingRunnable body) throws Exception {
        GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        mutation.accept(wms);
        gs.save(wms);
        try {
            body.run();
        } finally {
            revertService(WMSInfo.class, null);
        }
    }

    /** Runs the body with every filter language conformance class turned off, so no filter can be parsed. */
    protected void withFilterLanguagesDisabled(ThrowingRunnable body) throws Exception {
        withWms(
                wms -> {
                    CQL2Conformance cql2 = CQL2Conformance.configuration(wms);
                    cql2.setText(false);
                    cql2.setJSON(false);
                    ECQLConformance.configuration(wms).setText(false);
                },
                body);
    }

    /** Asserts the request returns a 400 whose error body names the offending parameter. */
    protected void assertBadRequestMentions(String url, String parameter) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(url);
        assertEquals(400, response.getStatus());
        assertThat(response.getContentAsString(), containsString(parameter));
    }

    /**
     * Asserts the request fails as an invalid parameter value, with a message naming what the client got wrong, and
     * returns the error document for any further assertion.
     */
    protected DocumentContext assertInvalidParameter(String url, String expectedMessagePart) throws Exception {
        DocumentContext json = getAsJSONPath(url, 400);
        assertEquals(APIException.INVALID_PARAMETER_VALUE, json.read("type"));
        assertThat(json.read("title", String.class), containsString(expectedMessagePart));
        return json;
    }

    /** Opaque colours as {@link java.awt.image.BufferedImage#getRGB} returns them, alpha in the high byte. */
    protected static final int RED = 0xFFFF0000;

    protected static final int GREEN = 0xFF00FF00;

    protected static final int BLUE = 0xFF0000FF;

    protected static final int WHITE = 0xFFFFFFFF;

    protected static final int CORNFLOWER_BLUE = 0xFF6495ED;

    protected static final int MID_BLUE = 0xFF3366CC;

    protected static final int BLACK = 0xFF000000;

    /** White with a zero alpha channel: what a transparent map leaves where nothing is drawn. */
    protected static final int TRANSPARENT_WHITE = 0x00FFFFFF;

    /** Red with a zero alpha channel: the colour a transparent map keeps under the alpha. */
    protected static final int TRANSPARENT_RED = 0x00FF0000;

    /** The fill the default Lakes style gives Blue Lake. */
    protected static final int LAKE_BLUE = 0xFF4040C0;

    /** A pixel inside Blue Lake, in a 100x100 map of {@link #LAKE_WINDOW}. */
    protected static final int LAKE_X = 50;

    protected static final int LAKE_Y = 64;

    /** A window tight on the CITE data, where Blue Lake and several other test layers hold features. */
    protected static final String LAKE_WINDOW = "bbox=-0.002,-0.003,0.005,0.002&width=100&height=100";

    /** Reads a map or legend response as PNG, checking the media type and the encoded bytes. */
    protected BufferedImage getAsPNG(String path) throws Exception {
        return readImage(getAsServletResponse(path), "image/png", "png");
    }

    /** Reads a map response as JPEG, checking the media type and the encoded bytes. */
    protected BufferedImage getAsJPEG(String path) throws Exception {
        return readImage(getAsServletResponse(path), "image/jpeg", "jpeg");
    }

    /** Reads a map response as TIFF, checking the media type and the encoded bytes. */
    protected BufferedImage getAsTIFF(String path) throws Exception {
        // the imageio-ext reader names the format "tif", not "tiff"
        return readImage(getAsServletResponse(path), "image/tiff", "tif");
    }

    /**
     * Decodes an image response, checking both the declared media type and the format the bytes are actually in.
     *
     * @param format the ImageIO format name, matched ignoring case
     */
    protected BufferedImage readImage(MockHttpServletResponse response, String mediaType, String format)
            throws Exception {
        assertEquals(200, response.getStatus());
        assertEquals(mediaType, getBaseMimeType(response.getContentType()));
        try (ImageInputStream input =
                ImageIO.createImageInputStream(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            assertTrue("Response bytes are not a readable image", readers.hasNext());
            ImageReader reader = readers.next();
            try {
                assertEquals(format.toLowerCase(), reader.getFormatName().toLowerCase());
                reader.setInput(input);
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        }
    }

    /**
     * Alpha of one pixel, 0 fully transparent and 255 fully opaque. {@link BufferedImage#getRGB} returns the pixel as
     * ARGB with alpha in the high byte.
     */
    protected static int alpha(BufferedImage image, int x, int y) {
        return alpha(image.getRGB(x, y));
    }

    /** Alpha of an ARGB pixel as returned by {@link BufferedImage#getRGB}, 0 fully transparent and 255 fully opaque. */
    protected static int alpha(int argb) {
        return argb >>> 24;
    }

    /** Red band of an ARGB pixel, 0 to 255. */
    protected static int red(int argb) {
        return (argb >> 16) & 0xFF;
    }

    /** Green band of an ARGB pixel, 0 to 255. */
    protected static int green(int argb) {
        return (argb >> 8) & 0xFF;
    }

    /** Blue band of an ARGB pixel, 0 to 255. */
    protected static int blue(int argb) {
        return argb & 0xFF;
    }

    /** The three colour bands of an ARGB pixel, with the alpha dropped, for comparisons that ignore opacity. */
    protected static int rgb(int argb) {
        return argb & 0xFFFFFF;
    }

    /** Asserts the pixel at the given x,y holds rendered data. */
    protected static void assertOpaque(BufferedImage image, int[] xy) {
        assertNotEquals("expected rendered data at " + xy[0] + "," + xy[1], 0, alpha(image, xy[0], xy[1]));
    }

    /** Asserts the pixel at the given x,y was left empty. */
    protected static void assertTransparent(BufferedImage image, int[] xy) {
        assertEquals("expected no data at " + xy[0] + "," + xy[1], 0, alpha(image, xy[0], xy[1]));
    }

    /** All the pixels of an image, row by row, as ARGB values. */
    protected static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    /**
     * sf:TimeWithStartEnd holds three features, one per world quadrant: {@code startElevation=1.0} covers NW and SW,
     * {@code startElevation=2.0} covers NE, and only NE carries the second timestamp. The pixels below sit inside each
     * quadrant of a 50x50 map of the whole world, so a selection that drops a feature leaves its quadrant empty.
     */
    protected static final int[] NE = {37, 12};

    protected static final int[] NW = {12, 12};

    protected static final int[] SW = {12, 37};

    /** A 50x50 transparent map of the whole world over sf:TimeWithStartEnd, with the given extra query string. */
    protected static String quadrantMapUrl(String query) {
        return "ogc/maps/v1/collections/sf:TimeWithStartEnd/map?f=image/png&width=50&height=50"
                + "&bbox=-180,-90,180,90&transparent=true"
                + (query.isEmpty() ? "" : "&" + query);
    }

    /** The map {@link #quadrantMapUrl} describes, decoded. */
    protected BufferedImage quadrantMap(String query) throws Exception {
        return getAsPNG(quadrantMapUrl(query));
    }

    /** A feature info request on the {@link #NE} pixel of the very same map. */
    protected static String quadrantInfoUrl(String query) {
        return "ogc/maps/v1/collections/sf:TimeWithStartEnd/map/info?f=application%2Fjson&width=50&height=50"
                + "&bbox=-180,-90,180,90&i=" + NE[0] + "&j=" + NE[1]
                + (query.isEmpty() ? "" : "&" + query);
    }

    /** Creates and saves a layer group with the given contents, each in its default style. */
    protected LayerGroupInfo addLayerGroup(String name, LayerGroupInfo.Mode mode, PublishedInfo... contents)
            throws Exception {
        Catalog catalog = getCatalog();
        LayerGroupInfo group = catalog.getFactory().createLayerGroup();
        group.setName(name);
        if (mode != null) group.setMode(mode);
        for (PublishedInfo content : contents) {
            group.getLayers().add(content);
            group.getStyles().add(null);
        }
        new CatalogBuilder(catalog).calculateLayerGroupBounds(group);
        catalog.add(group);
        // the catalog copy, the only one that can be modified and saved again
        return catalog.getLayerGroupByName(name);
    }

    /** The name most tests give the Lakes and Forests layer group. */
    protected static final String NATURE_GROUP = "nature";

    /** A layer group of Lakes drawn below Forests, both in their default style. */
    protected LayerGroupInfo addNatureGroup(String name) throws Exception {
        return addLayerGroup(name, null, layer(MockData.LAKES), layer(MockData.FORESTS));
    }

    /** The catalog layer publishing a test data type. */
    protected LayerInfo layer(QName typeName) {
        return getCatalog().getLayerByName(getLayerId(typeName));
    }

    protected void setupStartEndTimeDimension(QName typeName, String dimension, String start, String end) {
        FeatureTypeInfo info = getCatalog().getFeatureTypeByName(typeName.getLocalPart());
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setAttribute(start);
        di.setEndAttribute(end);
        di.setPresentation(DimensionPresentation.LIST);
        info.getMetadata().put(dimension, di);
        getCatalog().save(info);
    }
}
