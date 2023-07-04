/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import net.opengis.wcs20.GetCoverageType;
import net.opengis.wcs20.ScalingType;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs.responses.GeoTIFFCoverageResponseDelegate;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geoserver.wcs2_0.kvp.WCS20GetCoverageRequestReader;
import org.geoserver.wcs2_0.response.MIMETypeMapper;
import org.geoserver.wcs2_0.util.EnvelopeAxesLabelsMapper;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Testing WCS 2.0 Core {@link GetCoverage}
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 * @author Emanuele Tajariol, GeoSolutions SAS
 */
public class GetCoverageTest extends WCSTestSupport {

    private static final QName RAIN = new QName(MockData.SF_URI, "rain", MockData.SF_PREFIX);
    private static final QName TIMESERIES =
            new QName(MockData.SF_URI, "timeseries", MockData.SF_PREFIX);
    /** GEOS-11033: Test resource where native bounding box ReferencedEnvelope crs missing. */
    public static QName NO_ENVELOPE_SRS =
            new QName(MockData.WCS_URI, "NoEnvelopeSRS", MockData.WCS_PREFIX);

    private GridCoverage2DReader coverageReader;
    private AffineTransform2D originalMathTransform;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        testData.addRasterLayer(RAIN, "rain.zip", "asc", getCatalog());
        testData.addRasterLayer(TIMESERIES, "timeseries.zip", null, getCatalog());
        setupRasterDimension(
                TIMESERIES, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);

        testData.addRasterLayer(
                NO_ENVELOPE_SRS, "/world.tiff", null, null, GetCoverageTest.class, getCatalog());
        CoverageInfo noEnvelopeSRS = getCatalog().getCoverageByName(getLayerId(NO_ENVELOPE_SRS));
        ReferencedEnvelope bbox = noEnvelopeSRS.getNativeBoundingBox();
        noEnvelopeSRS.setNativeBoundingBox(ReferencedEnvelope.create(bbox, null));
        noEnvelopeSRS.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
        getCatalog().save(noEnvelopeSRS);
    }

    @Before
    public void getRainReader() throws IOException {
        // get the original transform
        CoverageInfo ci = getCatalog().getCoverageByName(getLayerId(RAIN));
        coverageReader = (GridCoverage2DReader) ci.getGridCoverageReader(null, null);
        originalMathTransform =
                (AffineTransform2D) coverageReader.getOriginalGridToWorld(PixelInCell.CELL_CORNER);
    }

    @Test
    public void testAllowSubsamplingOnScaleFactor() throws Exception {
        // setup a request
        Map<String, Object> raw = setupGetCoverageRain();
        raw.put("scalefactor", "0.5");
        assertScalingByHalf(raw);
    }

    @Test
    public void testAllowSubsamplingOnScaleExtent() throws Exception {
        GridEnvelope range = coverageReader.getOriginalGridRange();
        int width = range.getSpan(0);
        int height = range.getSpan(1);

        // setup a request
        Map<String, Object> raw = setupGetCoverageRain();
        raw.put("scaleextent", "i(0," + (width / 2) + "),j(0," + (height / 2) + ")");
        assertScalingByHalf(raw);
    }

    @Test
    public void getNearestTime() throws Exception {
        // get the original transform
        CoverageInfo ci = getCatalog().getCoverageByName(getLayerId(TIMESERIES));
        GetCoverageType gc =
                parse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=timeseries&subset=time(\"2019-01-03T00:00:00Z\")");
        coverageReader = (GridCoverage2DReader) ci.getGridCoverageReader(null, null);
        WCSInfo service = getGeoServer().getService(WCSInfo.class);
        EnvelopeAxesLabelsMapper axesMapper =
                GeoServerExtensions.bean(EnvelopeAxesLabelsMapper.class);
        MIMETypeMapper mimeMapper = GeoServerExtensions.bean(MIMETypeMapper.class);
        GetCoverage getCoverage = new GetCoverage(service, getCatalog(), axesMapper, mimeMapper);
        boolean hasCoverage = true;
        GridCoverage gridCoverage = null;
        try {
            // Requested a missing time will thrown an exception of requested time out of range
            gridCoverage = getCoverage.run(gc);
        } catch (WCS20Exception e) {
            hasCoverage = false;
            assertTrue(e.getMessage().contains("Requested time subset does not intersect"));
            assertEquals(404, (int) e.getHttpCode());
        }
        assertFalse(hasCoverage);

        // Enabling nearestMatch on timeDimension
        setupNearestMatch(TIMESERIES, "time", true, "P5D", true);
        getCoverage = new GetCoverage(service, getCatalog(), axesMapper, mimeMapper);
        try {
            gridCoverage = getCoverage.run(gc);
            hasCoverage = true;
        } catch (WCS20Exception e) {
            hasCoverage = false;
        }
        assertNotNull(gridCoverage);
        assertTrue(hasCoverage);
        scheduleForCleaning(gridCoverage);
    }

    @Test
    public void testDeflateCompressionLevel() throws Exception {
        CoverageInfo ci = getCatalog().getCoverageByName(getLayerId(TIMESERIES));
        GetCoverageType gc =
                parse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=timeseries&subset=time(\"2018-01-01T00:00:00Z\")"
                                + "&format=image/tiff&geotiff:compression=DEFLATE");
        coverageReader = (GridCoverage2DReader) ci.getGridCoverageReader(null, null);
        GeoServer geoserver = getGeoServer();
        WCSInfo service = geoserver.getService(WCSInfo.class);
        service.setDefaultDeflateCompressionLevel(9);
        geoserver.save(service);
        EnvelopeAxesLabelsMapper axesMapper =
                GeoServerExtensions.bean(EnvelopeAxesLabelsMapper.class);
        MIMETypeMapper mimeMapper = GeoServerExtensions.bean(MIMETypeMapper.class);

        GetCoverage getCoverage = new GetCoverage(service, getCatalog(), axesMapper, mimeMapper);
        GridCoverage gridCoverage = getCoverage.run(gc);
        GeoTIFFCoverageResponseDelegate delegate =
                new GeoTIFFCoverageResponseDelegate(getGeoServer());
        long small = getEncodingLength(delegate, gridCoverage);
        service = geoserver.getService(WCSInfo.class);
        service.setDefaultDeflateCompressionLevel(1);
        geoserver.save(service);
        long big = getEncodingLength(delegate, gridCoverage);
        assertTrue(big > small);
        scheduleForCleaning(gridCoverage);
    }

    private long getEncodingLength(
            GeoTIFFCoverageResponseDelegate delegate, GridCoverage gridCoverage)
            throws IOException {
        File file = File.createTempFile("wcs", "deflate.tif");
        file.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(file)) {

            delegate.encode(
                    (GridCoverage2D) gridCoverage,
                    "image/tiff",
                    Collections.singletonMap("compression", "DEFLATE"),
                    fos);
            return file.length();
        }
    }

    protected GetCoverageType parse(String url) throws Exception {
        Map<String, Object> rawKvp = new CaseInsensitiveMap<>(KvpUtils.parseQueryString(url));
        Map<String, Object> kvp = new CaseInsensitiveMap<>(parseKvp(rawKvp));
        WCS20GetCoverageRequestReader reader = new WCS20GetCoverageRequestReader();
        GetCoverageType gc = (GetCoverageType) reader.createRequest();
        return (GetCoverageType) reader.read(gc, kvp, rawKvp);
    }

    private void assertScalingByHalf(Map<String, Object> raw) throws Exception {
        Map<String, Object> kvp = parseKvp(raw);
        WCS20GetCoverageRequestReader reader = new WCS20GetCoverageRequestReader();
        GetCoverageType getCoverageRequest =
                (GetCoverageType) reader.read(reader.createRequest(), kvp, raw);

        // setup a getcoverage object we can observe
        WCSInfo service = getGeoServer().getService(WCSInfo.class);
        EnvelopeAxesLabelsMapper axesMapper =
                GeoServerExtensions.bean(EnvelopeAxesLabelsMapper.class);
        MIMETypeMapper mimeMapper = GeoServerExtensions.bean(MIMETypeMapper.class);
        GetCoverage getCoverage =
                new GetCoverage(service, getCatalog(), axesMapper, mimeMapper) {
                    @Override
                    MathTransform getMathTransform(
                            CoverageInfo ci,
                            GridCoverage2DReader reader,
                            Envelope subset,
                            GridCoverageRequest request,
                            PixelInCell pixelInCell,
                            ScalingType scaling)
                            throws IOException {
                        MathTransform mt =
                                super.getMathTransform(
                                        ci, reader, subset, request, pixelInCell, scaling);

                        // check we are giving the reader the expected scaling factor
                        AffineTransform2D actual = (AffineTransform2D) mt;
                        assertEquals(
                                0.5, originalMathTransform.getScaleX() / actual.getScaleX(), 1e-6);
                        assertEquals(
                                0.5, originalMathTransform.getScaleY() / actual.getScaleY(), 1e-6);

                        return mt;
                    }
                };
        GridCoverage result = getCoverage.run(getCoverageRequest);
        scheduleForCleaning(result);
    }

    private Map<String, Object> setupGetCoverageRain() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("service", "WCS");
        raw.put("request", "GetCoverage");
        raw.put("version", "2.0.1");
        raw.put("coverageId", "sf__rain");
        raw.put("format", "image/tiff");
        return raw;
    }

    @Test
    public void testInvalidTimeSpecification() throws Exception {
        // day is expressed as a single number instead of 2
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=timeseries&subset=time(\"2018-01-1T00:00:00Z\")");
        String error = checkOws20Exception(response, 400, "InvalidEncodingSyntax", "subset");
        assertThat(error, CoreMatchers.containsString("Invalid time subset"));
        assertThat(error, CoreMatchers.containsString("2018-01-1T00:00:00Z"));
    }

    @Test
    public void testInvalidLongSpecification() throws Exception {
        // longitude expressed as a string
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=timeseries&subset=Long(abc)");
        String error = checkOws20Exception(response, 400, "InvalidEncodingSyntax", "subset");
        assertThat(error, CoreMatchers.containsString("Invalid point value"));
        assertThat(error, CoreMatchers.containsString("abc"));
    }

    @Test
    public void testNoEnvelopeSRS() throws Exception {
        CoverageInfo ci = getCatalog().getCoverageByName(getLayerId(NO_ENVELOPE_SRS));
        GetCoverageType gc =
                parse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=NoEnvelopeSRS&format=image/tiff");

        coverageReader = (GridCoverage2DReader) ci.getGridCoverageReader(null, null);
        WCSInfo service = getGeoServer().getService(WCSInfo.class);
        EnvelopeAxesLabelsMapper axesMapper =
                GeoServerExtensions.bean(EnvelopeAxesLabelsMapper.class);
        MIMETypeMapper mimeMapper = GeoServerExtensions.bean(MIMETypeMapper.class);
        GetCoverage getCoverage = new GetCoverage(service, getCatalog(), axesMapper, mimeMapper);
        boolean hasCoverage = true;
        GridCoverage gridCoverage = null;
        try {
            gridCoverage = getCoverage.run(gc);
        } catch (WCS20Exception e) {
            hasCoverage = false;
            assertEquals(404, (int) e.getHttpCode());
        }
        assertNotNull(gridCoverage);
        assertTrue(hasCoverage);
        scheduleForCleaning(gridCoverage);
    }
}
