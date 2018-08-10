/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import net.opengis.wcs20.GetCoverageType;
import net.opengis.wcs20.ScalingType;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs2_0.kvp.WCS20GetCoverageRequestReader;
import org.geoserver.wcs2_0.response.MIMETypeMapper;
import org.geoserver.wcs2_0.util.EnvelopeAxesLabelsMapper;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.junit.Before;
import org.junit.Test;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;

/**
 * Testing WCS 2.0 Core {@link GetCoverage}
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 * @author Emanuele Tajariol, GeoSolutions SAS
 */
public class GetCoverageTest extends WCSTestSupport {

    private static final QName RAIN = new QName(MockData.SF_URI, "rain", MockData.SF_PREFIX);
    private GridCoverage2DReader coverageReader;
    private AffineTransform2D originalMathTransform;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        testData.addRasterLayer(RAIN, "rain.zip", "asc", getCatalog());
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
        Map<String, String> raw = setupGetCoverageRain();
        raw.put("scalefactor", "0.5");
        assertScalingByHalf(raw);
    }

    @Test
    public void testAllowSubsamplingOnScaleExtent() throws Exception {
        GridEnvelope range = coverageReader.getOriginalGridRange();
        int width = range.getSpan(0);
        int height = range.getSpan(1);

        // setup a request
        Map<String, String> raw = setupGetCoverageRain();
        raw.put("scaleextent", "i(0," + (width / 2) + "),j(0," + (height / 2) + ")");
        assertScalingByHalf(raw);
    }

    private void assertScalingByHalf(Map<String, String> raw) throws Exception {
        Map kvp = parseKvp(raw);
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
                            GridCoverage2DReader reader,
                            Envelope subset,
                            GridCoverageRequest request,
                            PixelInCell pixelInCell,
                            ScalingType scaling)
                            throws IOException {
                        MathTransform mt =
                                super.getMathTransform(
                                        reader, subset, request, pixelInCell, scaling);

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

    private Map<String, String> setupGetCoverageRain() {
        Map<String, String> raw = new HashMap<>();
        raw.put("service", "WCS");
        raw.put("request", "GetCoverage");
        raw.put("version", "2.0.1");
        raw.put("coverageId", "sf__rain");
        raw.put("format", "image/tiff");
        return raw;
    }
}
