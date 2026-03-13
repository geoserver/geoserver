package org.geoserver.pngwind;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.pngwind.config.PngWindConfigurator;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.util.NumberRange;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class PngWindWmsIntegrationTest extends WMSTestSupport {

    private static final String UV_LAYER = "winduv";
    private static final String SD_LAYER = "windspeeddir";
    private static final QName WIND_UV = new QName(MockData.SF_URI, UV_LAYER, MockData.SF_PREFIX);
    private static final QName WIND_SPEEDDIR = new QName(MockData.SF_URI, SD_LAYER, MockData.SF_PREFIX);

    private static final String OUTPUT_FORMAT = "image/vnd.png-wind";
    private static final double DELTA = 1E-6;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addRasterLayer(WIND_UV, "test-data/wind-uv.tif", "tif", new HashMap<>(), getClass(), getCatalog());
        testData.addRasterLayer(
                WIND_SPEEDDIR, "test-data/wind-speeddir.tif", "tif", new HashMap<>(), getClass(), getCatalog());
        configureCoverageDimensions(
                UV_LAYER,
                new String[] {"u", "v"},
                new String[] {"m/s", "m/s"},
                new Double[] {-20d, -20d},
                new Double[] {20d, 20d},
                new Double[] {-999d, -999d});

        configureCoverageDimensions(
                SD_LAYER,
                new String[] {"speed", "direction"},
                new String[] {"m/s", "deg"},
                new Double[] {-20d, 0d},
                new Double[] {20d, 350d},
                new Double[] {Double.NaN, Double.NaN});
        PngWindConfigurator configurator = applicationContext.getBean(PngWindConfigurator.class);
        configurator.onReload();
    }

    private void configureCoverageDimensions(
            String layerName,
            String[] dimensionNames,
            String[] dimensionUnits,
            Double[] mins,
            Double[] maxs,
            Double[] nodataValues)
            throws IOException {
        Catalog catalog = getCatalog();

        CoverageInfo coverage = catalog.getCoverageByName(MockData.SF_PREFIX, layerName);
        assertNotNull("Coverage not found in catalog: " + MockData.SF_PREFIX + ":" + layerName, coverage);

        List<CoverageDimensionInfo> dimensions = coverage.getDimensions();
        assertEquals("Unexpected number of dimensions", dimensionNames.length, dimensions.size());

        for (int i = 0; i < dimensions.size(); i++) {
            CoverageDimensionInfo dim = dimensions.get(i);
            dim.setName(dimensionNames[i]);
            dim.setDescription(dimensionNames[i]);

            if (dimensionUnits != null && i < dimensionUnits.length) {
                dim.setUnit(dimensionUnits[i]);
            }
            if (mins != null && i < mins.length && mins[i] != null) {
                dim.setRange(new NumberRange<>(Double.class, mins[i], maxs[i]));
            } else if (maxs != null && i < maxs.length && maxs[i] != null) {
                dim.setRange(new NumberRange<>(Double.class, mins[i], maxs[i]));
            }
            if (nodataValues != null && i < nodataValues.length && nodataValues[i] != null) {
                dim.getNullValues().clear();
                dim.getNullValues().add(nodataValues[i]);
            }
        }

        catalog.save(coverage);
    }

    @Test
    public void testGetMapPngWindFromUvCoverage() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?service=WMS&version=1.1.1"
                + "&request=GetMap"
                + "&layers=" + MockData.SF_PREFIX + ":" + UV_LAYER
                + "&styles="
                + "&srs=EPSG:4326"
                + "&bbox=0,0,20,20"
                + "&width=10"
                + "&height=10"
                + "&format=" + PngWindConstants.MIME_TYPE);

        assertEquals(200, response.getStatus());
        assertEquals(OUTPUT_FORMAT, response.getContentType());

        byte[] png = response.getContentAsByteArray();
        assertTrue(png.length > 0);

        Map<String, String> textChunks = readPngTextChunks(png);
        assertEquals(PngWindConstants.FORMAT, textChunks.get("format"));
        assertEquals("u", textChunks.get("wind_b1_name"));
        assertEquals("v", textChunks.get("wind_b2_name"));
        assertEquals(-20d, Double.parseDouble(textChunks.get("wind_b1_offset")), DELTA);
        assertEquals(0.156862d, Double.parseDouble(textChunks.get("wind_b1_scale")), DELTA);
        assertEquals(-20d, Double.parseDouble(textChunks.get("wind_b2_offset")), DELTA);
        assertEquals(0.156862d, Double.parseDouble(textChunks.get("wind_b2_scale")), DELTA);
        assertEquals("m/s", textChunks.get("wind_b1_uom"));
        assertEquals("m/s", textChunks.get("wind_b2_uom"));
        assertEquals("EPSG:4326", textChunks.get("CRS"));
        assertEquals("0.000000,0.000000,20.000000,20.000000", textChunks.get("bbox"));
    }

    @Test
    public void testGetMapPngWindFromSpeedDirectionCoverage() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?service=WMS&version=1.1.1"
                + "&request=GetMap"
                + "&layers=" + MockData.SF_PREFIX + ":" + SD_LAYER
                + "&styles="
                + "&srs=EPSG:4326"
                + "&bbox=0,0,20,20"
                + "&width=20"
                + "&height=20"
                + "&format=" + OUTPUT_FORMAT);

        assertEquals(200, response.getStatus());
        assertEquals(OUTPUT_FORMAT, response.getContentType());

        byte[] png = response.getContentAsByteArray();
        assertTrue(png.length > 0);
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(png))) {
            ImageReader reader = ImageIO.getImageReadersByFormatName("png").next();
            reader.setInput(in);
            assertEquals(20, reader.getWidth(0));
            assertEquals(20, reader.getHeight(0));
            BufferedImage image = reader.read(0);
            Raster data = image.getData();
            for (int y = 0; y < data.getHeight(); y++) {
                // The pixel values on the edges should be nodata, therefore 0 as the 3rd band
                assertEquals(0, data.getSample(0, y, 2));
                assertEquals(0, data.getSample(19, y, 2));
            }
        }

        Map<String, String> textChunks = readPngTextChunks(png);
        assertEquals(PngWindConstants.FORMAT, textChunks.get("format"));
        assertEquals("U", textChunks.get("wind_b1_name"));
        assertEquals("V", textChunks.get("wind_b2_name"));
        assertEquals(-20d, Double.parseDouble(textChunks.get("wind_b1_offset")), DELTA);
        assertEquals(0.156862d, Double.parseDouble(textChunks.get("wind_b1_scale")), DELTA);
        assertEquals(-20d, Double.parseDouble(textChunks.get("wind_b2_offset")), DELTA);
        assertEquals(0.156862d, Double.parseDouble(textChunks.get("wind_b2_scale")), DELTA);
        assertEquals("m/s", textChunks.get("wind_b1_uom"));
        assertEquals("m/s", textChunks.get("wind_b2_uom"));
        assertEquals("EPSG:4326", textChunks.get("CRS"));
        assertEquals("0.000000,0.000000,20.000000,20.000000", textChunks.get("bbox"));
    }

    @Test
    public void testUnsupportedOnMultipleLayers() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?service=WMS"
                + "&version=1.1.1"
                + "&request=GetMap"
                + "&layers=" + MockData.SF_PREFIX + ":" + UV_LAYER + "," + MockData.SF_PREFIX + ":" + SD_LAYER
                + "&styles=,"
                + "&srs=EPSG:4326"
                + "&bbox=10,40,14,44"
                + "&width=4"
                + "&height=4"
                + "&format=" + OUTPUT_FORMAT);

        assertTrue(response.getStatus() >= 400 || isOwsException(response));
        assertPngWindUnsupported(response, "single");
    }

    @Test
    public void testUnsupportedOnVectorLayer() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?service=WMS"
                + "&version=1.1.1"
                + "&request=GetMap"
                + "&layers=" + MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart()
                + "&styles="
                + "&srs=EPSG:4326"
                + "&bbox=-180,-90,180,90"
                + "&width=10"
                + "&height=10"
                + "&format=" + OUTPUT_FORMAT);

        assertTrue(response.getStatus() >= 400 || isOwsException(response));
        assertPngWindUnsupported(response, "raster");
    }

    @Test
    public void testUnsupportedOnMoreThan2Bands() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?service=WMS"
                + "&version=1.1.1"
                + "&request=GetMap"
                + "&layers=" + MockData.WORLD.getPrefix() + ":" + MockData.WORLD.getLocalPart()
                + "&styles="
                + "&srs=EPSG:4326"
                + "&bbox=-180,-90,180,90"
                + "&width=10"
                + "&height=10"
                + "&format=" + OUTPUT_FORMAT);

        assertTrue(response.getStatus() >= 400 || isOwsException(response));
        assertPngWindUnsupported(response, "requires a 2-band raster" + "" + "");
    }

    private static boolean isOwsException(MockHttpServletResponse response) throws IOException {
        String content = response.getContentAsString();
        return content != null && (content.contains("ExceptionReport") || content.contains("ServiceExceptionReport"));
    }

    private static void assertPngWindUnsupported(MockHttpServletResponse response, String expectedText)
            throws IOException {
        String content = response.getContentAsString();
        assertNotNull(content);
        assertTrue(
                "Expected failure mentioning '" + expectedText + "' but got: " + content,
                content.toLowerCase().contains(expectedText.toLowerCase()));
    }

    /** Reads PNG tEXt chunks without depending on PNG metadata DOM parsing. */
    private static Map<String, String> readPngTextChunks(byte[] png) throws IOException {
        Map<String, String> result = new LinkedHashMap<>();

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(png))) {
            byte[] signature = new byte[8];
            in.readFully(signature);
            assertArrayEquals(new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10}, signature);

            while (true) {
                int length = in.readInt();
                byte[] typeBytes = new byte[4];
                in.readFully(typeBytes);
                String type = new String(typeBytes, StandardCharsets.ISO_8859_1);

                byte[] data = new byte[length];
                in.readFully(data);
                in.readInt(); // CRC

                if ("tEXt".equals(type)) {
                    int sep = -1;
                    for (int i = 0; i < data.length; i++) {
                        if (data[i] == 0) {
                            sep = i;
                            break;
                        }
                    }
                    if (sep > 0) {
                        String key = new String(data, 0, sep, StandardCharsets.ISO_8859_1);
                        String value = new String(data, sep + 1, data.length - sep - 1, StandardCharsets.ISO_8859_1);
                        result.put(key, value);
                    }
                }

                if ("IEND".equals(type)) {
                    break;
                }
            }
        }

        return result;
    }
}
