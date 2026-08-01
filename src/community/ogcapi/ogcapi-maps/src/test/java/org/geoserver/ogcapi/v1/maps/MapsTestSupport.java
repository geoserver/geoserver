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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.function.BiConsumer;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
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

    /** Asserts the request returns a 400 whose error body names the offending parameter. */
    protected void assertBadRequestMentions(String url, String parameter) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(url);
        assertEquals(400, response.getStatus());
        assertThat(response.getContentAsString(), containsString(parameter));
    }

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

    /** Asserts the pixel at the given x,y holds rendered data. */
    protected static void assertOpaque(BufferedImage image, int[] xy) {
        assertNotEquals("expected rendered data at " + xy[0] + "," + xy[1], 0, image.getRGB(xy[0], xy[1]) >>> 24);
    }

    /** Asserts the pixel at the given x,y was left empty. */
    protected static void assertTransparent(BufferedImage image, int[] xy) {
        assertEquals("expected no data at " + xy[0] + "," + xy[1], 0, image.getRGB(xy[0], xy[1]) >>> 24);
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
