/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.kvp;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.geom.AffineTransform;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import javax.xml.namespace.QName;
import net.opengis.wcs20.GetCoverageType;
import net.opengis.wcs20.InterpolationType;
import net.opengis.wcs20.RangeItemType;
import net.opengis.wcs20.RangeSubsetType;
import net.opengis.wcs20.ScaleAxisByFactorType;
import net.opengis.wcs20.ScaleAxisType;
import net.opengis.wcs20.ScaleByFactorType;
import net.opengis.wcs20.ScaleToExtentType;
import net.opengis.wcs20.ScaleToSizeType;
import net.opengis.wcs20.ScalingType;
import net.opengis.wcs20.TargetAxisExtentType;
import net.opengis.wcs20.TargetAxisSizeType;
import org.apache.commons.io.FileUtils;
import org.eclipse.emf.common.util.EList;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wcs2_0.WCS20Const;
import org.geotools.api.data.DataSourceException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.GeneralBounds;
import org.geotools.image.test.ImageAssert;
import org.geotools.referencing.CRS;
import org.geotools.wcs.v2_0.RangeSubset;
import org.geotools.wcs.v2_0.Scaling;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class GetCoverageKvpTest extends WCSKVPTestSupport {

    private static final QName WORLD_EXTRA = new QName(MockData.SF_URI, "world", MockData.SF_PREFIX);
    private static final QName RAIN = new QName(MockData.SF_URI, "rain", MockData.SF_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addRasterLayer(RAIN, "rain.zip", "asc", getCatalog());
        testData.addRasterLayer(
                new QName(MockData.SF_URI, "mosaic", MockData.SF_PREFIX),
                "raster-filter-test.zip",
                null,
                null,
                SystemTestData.class,
                getCatalog());
        // a world layer, whose bbox goes slightly outside the dateline
        testData.addRasterLayer(WORLD_EXTRA, "world.tiff", "tiff", getCatalog());
    }

    @Test
    public void testParseBasic() throws Exception {
        GetCoverageType gc = parse("wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=theCoverage");

        assertEquals("theCoverage", gc.getCoverageId());
    }

    @Test
    public void testGetCoverageNoWs() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                "wcs?request=GetCoverage&service=WCS&version=2.0.1" + "&coverageId=BlueMarble&Format=image/tiff");

        assertEquals("image/tiff", response.getContentType());
    }

    @Test
    public void testNotExistent() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                "wcs?request=GetCoverage&service=WCS&version=2.0.1" + "&coverageId=NotThere&&Format=image/tiff");
        checkOws20Exception(response, 404, "NoSuchCoverage", "coverageId");
    }

    @Test
    public void testGetCoverageLocalWs() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                "wcs/wcs?request=GetCoverage&service=WCS&version=2.0.1" + "&coverageId=BlueMarble&&Format=image/tiff");

        assertEquals("image/tiff", response.getContentType());
    }

    @Test
    public void testExtensionScaleFactor() throws Exception {
        GetCoverageType gc =
                parse("wcs?request=GetCoverage&service=WCS&version=2.0.1" + "&coverageId=theCoverage&scaleFactor=2");

        Map<String, Object> extensions = getExtensionsMap(gc);

        assertEquals(1, extensions.size());
        ScalingType scaling = (ScalingType) extensions.get(Scaling.NAMESPACE + ":Scaling");
        ScaleByFactorType sbf = scaling.getScaleByFactor();
        assertEquals(2.0, sbf.getScaleFactor(), 0d);
    }

    @Test
    public void testExtensionScaleAxes() throws Exception {
        GetCoverageType gc = parse("wcs?request=GetCoverage&service=WCS&version=2.0.1"
                + "&coverageId=theCoverage&scaleaxes=http://www.opengis.net/def/axis/OGC/1/i(3.5),"
                + "http://www.opengis.net/def/axis/OGC/1/j(5.0),http://www.opengis.net/def/axis/OGC/1/k(2.0)");

        Map<String, Object> extensions = getExtensionsMap(gc);
        assertEquals(1, extensions.size());
        ScalingType scaling = (ScalingType) extensions.get(Scaling.NAMESPACE + ":Scaling");
        ScaleAxisByFactorType sax = scaling.getScaleAxesByFactor();
        EList<ScaleAxisType> saxes = sax.getScaleAxis();
        assertEquals(3, saxes.size());
        assertEquals("http://www.opengis.net/def/axis/OGC/1/i", saxes.get(0).getAxis());
        assertEquals(3.5d, saxes.get(0).getScaleFactor(), 0d);
        assertEquals("http://www.opengis.net/def/axis/OGC/1/j", saxes.get(1).getAxis());
        assertEquals(5.0d, saxes.get(1).getScaleFactor(), 0d);
        assertEquals("http://www.opengis.net/def/axis/OGC/1/k", saxes.get(2).getAxis());
        assertEquals(2.0d, saxes.get(2).getScaleFactor(), 0d);
    }

    @Test
    public void testExtensionScaleSize() throws Exception {
        GetCoverageType gc = parse("wcs?request=GetCoverage&service=WCS&version=2.0.1"
                + "&coverageId=theCoverage&scalesize=http://www.opengis.net/def/axis/OGC/1/i(1000),"
                + "http://www.opengis.net/def/axis/OGC/1/j(1000),http://www.opengis.net/def/axis/OGC/1/k(10)");

        Map<String, Object> extensions = getExtensionsMap(gc);
        assertEquals(1, extensions.size());
        ScalingType scaling = (ScalingType) extensions.get(Scaling.NAMESPACE + ":Scaling");
        ScaleToSizeType sts = scaling.getScaleToSize();
        EList<TargetAxisSizeType> scaleAxes = sts.getTargetAxisSize();
        assertEquals(3, scaleAxes.size());
        assertEquals("http://www.opengis.net/def/axis/OGC/1/i", scaleAxes.get(0).getAxis());
        assertEquals(1000d, scaleAxes.get(0).getTargetSize(), 0d);
        assertEquals("http://www.opengis.net/def/axis/OGC/1/j", scaleAxes.get(1).getAxis());
        assertEquals(1000d, scaleAxes.get(1).getTargetSize(), 0d);
        assertEquals("http://www.opengis.net/def/axis/OGC/1/k", scaleAxes.get(2).getAxis());
        assertEquals(10d, scaleAxes.get(2).getTargetSize(), 0d);
    }

    @Test
    public void testExtensionScaleExtent() throws Exception {
        GetCoverageType gc = parse(
                "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                        + "&coverageId=theCoverage&scaleextent=http://www.opengis.net/def/axis/OGC/1/i(10,20),http://www.opengis.net/def/axis/OGC/1/j(20,30)");

        Map<String, Object> extensions = getExtensionsMap(gc);

        assertEquals(1, extensions.size());
        ScalingType scaling = (ScalingType) extensions.get(Scaling.NAMESPACE + ":Scaling");
        ScaleToExtentType ste = scaling.getScaleToExtent();
        assertEquals(2, ste.getTargetAxisExtent().size());
        TargetAxisExtentType tax = ste.getTargetAxisExtent().get(0);
        assertEquals("http://www.opengis.net/def/axis/OGC/1/i", tax.getAxis());
        assertEquals(10.0, tax.getLow(), 0d);
        assertEquals(20.0, tax.getHigh(), 0d);
        tax = ste.getTargetAxisExtent().get(1);
        assertEquals("http://www.opengis.net/def/axis/OGC/1/j", tax.getAxis());
        assertEquals(20.0, tax.getLow(), 0d);
        assertEquals(30.0, tax.getHigh(), 0d);
    }

    @Test
    public void testExtensionRangeSubset() throws Exception {
        GetCoverageType gc = parse("wcs?request=GetCoverage&service=WCS&version=2.0.1"
                + "&coverageId=theCoverage&rangesubset=band01,band03:band05,band10,band19:band21");

        Map<String, Object> extensions = getExtensionsMap(gc);

        assertEquals(1, extensions.size());
        RangeSubsetType rangeSubset = (RangeSubsetType) extensions.get(RangeSubset.NAMESPACE + ":RangeSubset");

        EList<RangeItemType> items = rangeSubset.getRangeItems();
        assertEquals(4, items.size());
        RangeItemType i1 = items.get(0);
        assertEquals("band01", i1.getRangeComponent());
        RangeItemType i2 = items.get(1);
        assertEquals("band03", i2.getRangeInterval().getStartComponent());
        assertEquals("band05", i2.getRangeInterval().getEndComponent());
        RangeItemType i3 = items.get(2);
        assertEquals("band10", i3.getRangeComponent());
        RangeItemType i4 = items.get(3);
        assertEquals("band19", i4.getRangeInterval().getStartComponent());
        assertEquals("band21", i4.getRangeInterval().getEndComponent());
    }

    @Test
    public void testExtensionCRS() throws Exception {
        GetCoverageType gc = parse(
                "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                        + "&coverageId=theCoverage&SUBSETTINGCRS=http://www.opengis.net/def/crs/EPSG/0/4326&outputcrs=http://www.opengis.net/def/crs/EPSG/0/32632");

        Map<String, Object> extensions = getExtensionsMap(gc);

        assertEquals(2, extensions.size());
        assertEquals(
                "http://www.opengis.net/def/crs/EPSG/0/4326",
                extensions.get("http://www.opengis.net/wcs/service-extension/crs/1.0:subsettingCrs"));
        assertEquals(
                "http://www.opengis.net/def/crs/EPSG/0/32632",
                extensions.get("http://www.opengis.net/wcs/service-extension/crs/1.0:outputCrs"));
    }

    @Test
    public void testExtensionInterpolationLinear() throws Exception {
        GetCoverageType gc = parse("wcs?request=GetCoverage&service=WCS&version=2.0.1"
                + "&coverageId=theCoverage&interpolation=http://www.opengis.net/def/interpolation/OGC/1/linear");

        Map<String, Object> extensions = getExtensionsMap(gc);

        InterpolationType interp = (InterpolationType)
                extensions.get("http://www.opengis.net/WCS_service-extension_interpolation/1.0:Interpolation");
        assertEquals(
                "http://www.opengis.net/def/interpolation/OGC/1/linear",
                interp.getInterpolationMethod().getInterpolationMethod());
    }

    @Test
    public void testExtensionInterpolationMixed() throws Exception {
        GetCoverageType gc = parse("wcs?request=GetCoverage&service=WCS&version=2.0.1"
                + "&coverageId=theCoverage&interpolation=http://www.opengis.net/def/interpolation/OGC/1/linear");

        Map<String, Object> extensions = getExtensionsMap(gc);

        InterpolationType interp = (InterpolationType)
                extensions.get("http://www.opengis.net/WCS_service-extension_interpolation/1.0:Interpolation");
        assertEquals(
                "http://www.opengis.net/def/interpolation/OGC/1/linear",
                interp.getInterpolationMethod().getInterpolationMethod());
    }

    @Test
    public void testExtensionOverview() throws Exception {
        GetCoverageType gc = parse(
                "wcs?request=GetCoverage&service=WCS&version=2.0.1" + "&coverageId=theCoverage&overviewPolicy=QUALITY");
        Map<String, Object> extensions = getExtensionsMap(gc);

        assertEquals(1, extensions.size());
        String overviewPolicy = (String) extensions.get(
                WCS20Const.OVERVIEW_POLICY_EXTENSION_NAMESPACE + ":" + WCS20Const.OVERVIEW_POLICY_EXTENSION);
        assertEquals(overviewPolicy, "QUALITY");
    }

    @Test
    public void testGetMissingCoverage() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=notThereBaby");

        checkOws20Exception(response, 404, "NoSuchCoverage", "coverageId");
    }

    @Test
    public void testCqlFilterRed() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                "wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=sf__mosaic&CQL_FILTER=location like 'red%25'");
        assertOriginPixelColor(response, new int[] {255, 0, 0});
    }

    @Test
    public void testCqlFilterGreen() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                "wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=sf__mosaic&CQL_FILTER=location like 'green%25'");
        assertOriginPixelColor(response, new int[] {0, 255, 0});
    }

    @Test
    public void testSortByLocationAscending() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                "wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=sf__mosaic&sortBy=location");
        // green is the lowest, lexicographically
        assertOriginPixelColor(response, new int[] {0, 255, 0});
    }

    @Test
    public void testSortByLocationDescending() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                "wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=sf__mosaic&sortBy=location D");
        // yellow is the highest, lexicographically
        assertOriginPixelColor(response, new int[] {255, 255, 0});
    }

    @Test
    @SuppressWarnings("PMD.UseAssertEqualsInsteadOfAssertTrue")
    public void testWorldOutsideDateline() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=sf__world");
        // got back a tiff
        assertEquals("image/tiff", response.getContentType());
        assertEquals(200, response.getStatus());

        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("world", "world.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // check the tiff structure is the one requested
        final GeoTiffReader reader = new GeoTiffReader(file);
        GridCoverage2D coverage = null;
        try {
            assertTrue(CRS.equalsIgnoreMetadata(reader.getCoordinateReferenceSystem(), EPSG_4326));
            assertEquals(720, reader.getOriginalGridRange().getSpan(0));
            assertEquals(360, reader.getOriginalGridRange().getSpan(1));
            coverage = reader.read(null);
            assertNotNull(coverage);

            GeneralBounds expected = new GeneralBounds(new double[] {-180.01, -90}, new double[] {180.01, 90});
            expected.setCoordinateReferenceSystem(EPSG_4326);

            final double scale = getScale(coverage);
            assertEnvelopeEquals(expected, scale, (GeneralBounds) coverage.getEnvelope(), scale);

        } finally {
            clean(reader, coverage);
        }
    }

    @Test
    @SuppressWarnings("PMD.UseAssertEqualsInsteadOfAssertTrue")
    public void testScalingWithRequestCrossingDateline() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=sf__world"
                        + "&subset=http://www.opengis.net/def/axis/OGC/0/Long(40,240)"
                        + "&subset=http://www.opengis.net/def/axis/OGC/0/Lat(-50,50)"
                        + "&format=image/tiff"
                        + "&SCALESIZE="
                        + "http://www.opengis.net/def/axis/OGC/1/"
                        + "i(400),"
                        + "http://www.opengis.net/def/axis/OGC/1/"
                        + "j(200)");
        // got back a tiff
        assertEquals("image/tiff", response.getContentType());
        assertEquals(200, response.getStatus());

        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("world", "world.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // check the tiff structure is the one requested
        final GeoTiffReader reader = new GeoTiffReader(file);
        GridCoverage2D coverage = null;
        try {
            assertEquals(400, reader.getOriginalGridRange().getSpan(0));
            assertEquals(200, reader.getOriginalGridRange().getSpan(1));
            coverage = reader.read(null);
            assertNotNull(coverage);

            GeneralBounds expected = new GeneralBounds(new double[] {40, -50}, new double[] {240, 50});
            expected.setCoordinateReferenceSystem(EPSG_4326);

            final double scale = getScale(coverage);
            assertEnvelopeEquals(expected, scale, (GeneralBounds) coverage.getEnvelope(), scale);
            RenderedImage image = coverage.getRenderedImage();
            ImageAssert.assertEquals(
                    new File("src/test/resources/org/geoserver/wcs2_0/dateline-world.png"), image, 250);
        } finally {
            clean(reader, coverage);
        }
    }

    private void assertOriginPixelColor(MockHttpServletResponse response, int[] expected)
            throws DataSourceException, IOException {
        assertEquals("image/tiff", response.getContentType());
        byte[] bytes = response.getContentAsByteArray();

        GeoTiffReader reader = new GeoTiffReader(new ByteArrayInputStream(bytes));
        GridCoverage2D coverage = reader.read(null);
        Raster raster = coverage.getRenderedImage().getData();
        int[] pixel = new int[3];
        raster.getPixel(0, 0, pixel);
        assertThat(pixel, equalTo(expected));
    }

    @Test
    public void testImposedBBOX() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=wcs__utm11");
        // got back a tiff
        assertEquals("image/tiff", response.getContentType());
        assertEquals(200, response.getStatus());

        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("utm11", "utm11.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // check the tiff structure is the one requested
        final GeoTiffReader reader = new GeoTiffReader(file);
        GridCoverage2D coverage = null;
        try {
            CoordinateReferenceSystem crs = CRS.decode("EPSG:26711");
            assertTrue(CRS.equalsIgnoreMetadata(reader.getCoordinateReferenceSystem(), crs));

            coverage = reader.read(null);
            assertNotNull(coverage);

            // resolution is the native one
            final double scale = getScale(coverage);
            assertEquals(256, scale, 0d);

            // expect the fitted bounding box
            GeneralBounds expected = new GeneralBounds(new double[] {440562, 3720758}, new double[] {471794, 3750966});
            expected.setCoordinateReferenceSystem(crs);
            assertEnvelopeEquals(expected, scale, (GeneralBounds) coverage.getEnvelope(), scale);

            // fitting adds a pixel left and right, removes one top and bottom
            assertEquals(122, reader.getOriginalGridRange().getSpan(0));
            assertEquals(118, reader.getOriginalGridRange().getSpan(1));

            // the added "nodata" (zero in this case) in the first and last cols
            Raster raster = coverage.getRenderedImage().getData();
            int[] px = new int[1];
            for (int j = 0; j < 118; j++) {
                raster.getPixel(0, j, px);
                assertEquals(0, px[0]);
                raster.getPixel(121, j, px);
                assertEquals(0, px[0]);
            }
        } finally {
            clean(reader, coverage);
        }
    }

    @Test
    public void testImposedBBOXRotated() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=wcs__RotatedCad");
        // got back a tiff
        assertEquals("image/tiff", response.getContentType());
        assertEquals(200, response.getStatus());

        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("rotatedCad", "rotatedCad.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // check the tiff structure is the one requested
        final GeoTiffReader reader = new GeoTiffReader(file);
        GridCoverage2D coverage = null;
        try {
            CoordinateReferenceSystem crs = CRS.decode("EPSG:3003");
            assertTrue(CRS.equalsIgnoreMetadata(reader.getCoordinateReferenceSystem(), crs));
            coverage = reader.read(null);
            assertNotNull(coverage);

            GeneralBounds expected =
                    new GeneralBounds(new double[] {5000000, 1402800}, new double[] {5000100, 1402900});
            expected.setCoordinateReferenceSystem(crs);
            assertEnvelopeEquals(expected, 1, (GeneralBounds) coverage.getEnvelope(), 1);

            // check scale more or less preserved
            double scale = getScale(coverage);
            assertEquals(0.11285131, scale, EPS);

            // square pixel area
            assertEquals(886, reader.getOriginalGridRange().getSpan(0));
            assertEquals(886, reader.getOriginalGridRange().getSpan(1));
        } finally {
            clean(reader, coverage);
        }
    }

    @Test
    @SuppressWarnings("PMD.SimplifiableTestAssertion")
    public void testDatelineCross() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                "wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=wcs__dateline_cross");
        // got back a tiff
        assertEquals("image/tiff", response.getContentType());
        assertEquals(200, response.getStatus());

        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("dateline", "dateline.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // check the tiff structure is the one requested
        final GeoTiffReader reader = new GeoTiffReader(file);
        GridCoverage2D coverage = null;
        try {
            CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
            assertTrue(CRS.equalsIgnoreMetadata(reader.getCoordinateReferenceSystem(), crs));

            coverage = reader.read(null);
            assertNotNull(coverage);

            // resolution is the native one
            final double scale = getScale(coverage);
            assertEquals(0.005, scale, 1e-4);

            // expect the fitted bounding box
            GeneralBounds expected = new GeneralBounds(new double[] {179.5, -84.272}, new double[] {180, -82.217});
            expected.setCoordinateReferenceSystem(crs);
            assertTrue(
                    "Equality failed, actual envelope was " + coverage.getEnvelope2D(),
                    expected.equals(coverage.getEnvelope2D(), 1e-4, false));

            // fitting adds a pixel left and right, removes one top and bottom
            assertEquals(100, reader.getOriginalGridRange().getSpan(0));
            assertEquals(411, reader.getOriginalGridRange().getSpan(1));

        } finally {
            clean(reader, coverage);
        }
    }

    @Test
    @SuppressWarnings("PMD.SimplifiableTestAssertion")
    public void testReprojected() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=cdf__usa");
        // got back a tiff
        assertEquals("image/tiff", response.getContentType());
        assertEquals(200, response.getStatus());

        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("usa", "usa.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // check the tiff structure is the one requested
        final GeoTiffReader reader = new GeoTiffReader(file);
        GridCoverage2D coverage = null;
        try {
            CoordinateReferenceSystem crs = CRS.decode("EPSG:3857");
            assertTrue(CRS.equalsIgnoreMetadata(reader.getCoordinateReferenceSystem(), crs));
            coverage = reader.read(null);
            assertNotNull(coverage);

            GeneralBounds expected =
                    new GeneralBounds(new double[] {-14570240, 6199732}, new double[] {-13790593, 7197101});
            expected.setCoordinateReferenceSystem(crs);
            assertTrue(expected.equals(coverage.getEnvelope(), 1, false));

            // check scale more or less preserved
            AffineTransform affineTransform = getAffineTransform(coverage);
            assertEquals(7796, affineTransform.getScaleX(), 1d);
            assertEquals(-9973, affineTransform.getScaleY(), 1d);

            // square pixel area
            assertEquals(100, reader.getOriginalGridRange().getSpan(0));
            assertEquals(100, reader.getOriginalGridRange().getSpan(1));
        } finally {
            clean(reader, coverage);
        }
    }

    @Test
    public void testIAUCoverageGeotiff() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=iau__Viking");
        // got back a tiff
        assertEquals("image/tiff", response.getContentType());
        assertEquals(200, response.getStatus());

        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("viking", "viking.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // check the tiff structure is the one requested
        final GeoTiffReader reader = new GeoTiffReader(file);
        GridCoverage2D coverage = null;
        try {
            CoordinateReferenceSystem crs = CRS.decode("IAU:49900");
            assertTrue(CRS.equalsIgnoreMetadata(reader.getCoordinateReferenceSystem(), crs));

            coverage = reader.read(null);
            assertNotNull(coverage);

            // resolution is the native one
            final double scale = getScale(coverage);
            assertEquals(27, scale, 1e-3);
        } finally {
            clean(reader, coverage);
        }
    }

    @Test
    public void testIAUCoverageGML() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                "wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=iau__Viking&format=application/gml%2Bxml");
        // got back a GML coverage
        assertEquals("application/gml+xml", response.getContentType());
        assertEquals(200, response.getStatus());

        Document dom = dom(new ByteArrayInputStream(getBinary(response)));
        NodeList nodes = xpath.getMatchingNodes("//@srsName", dom);
        assertEquals(4, nodes.getLength());
        for (int i = 0; i < nodes.getLength(); i++) {
            assertEquals(
                    "http://www.opengis.net/def/crs/IAU/0/49900", nodes.item(i).getNodeValue());
        }
    }

    @Test
    public void testClipWorldNoCRS() throws Exception {
        String halfWorld = "POLYGON((0 -90, 0 90, 180 90, 180 -90, 0 -90))";
        MockHttpServletResponse response = getAsServletResponse(
                "wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=sf__world&clip=" + halfWorld);

        testCoverage(response, "halfWorld", (reader, coverage) -> {
            CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
            assertTrue(CRS.equalsIgnoreMetadata(reader.getCoordinateReferenceSystem(), crs));

            GeneralBounds expected = new GeneralBounds(new double[] {0, -90}, new double[] {180, 90});
            expected.setCoordinateReferenceSystem(crs);
            assertEnvelopeEquals(expected, 1, (GeneralBounds) coverage.getEnvelope(), 1);
        });
    }

    @Test
    public void testClipWorldReprojected() throws Exception {
        String halfWorld = "SRID=4326;POLYGON((0 -85, 0 85, 180 85, 180 -85, 0 -85))";
        MockHttpServletResponse response = getAsServletResponse(
                "wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=sf__world&outputCrs=EPSG:3857&clip="
                        + halfWorld);

        testCoverage(response, "halfWorld", (reader, coverage) -> {
            CoordinateReferenceSystem crs = CRS.decode("EPSG:3857");
            assertTrue(CRS.equalsIgnoreMetadata(reader.getCoordinateReferenceSystem(), crs));

            GeneralBounds expected =
                    new GeneralBounds(new double[] {0, -1.99663728E7}, new double[] {2.00375083E7, 1.99663728E7});
            expected.setCoordinateReferenceSystem(crs);
            assertEnvelopeEquals(expected, 1, (GeneralBounds) coverage.getEnvelope(), 1e6);
        });
    }

    @Test
    public void testClipInvalidGeometryType() throws Exception {
        String point = "POINT(0 -85)";
        MockHttpServletResponse response = getAsServletResponse(
                "wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=sf__world&outputCrs=EPSG:3857&clip="
                        + point);
        checkOws20Exception(response, 400, "InvalidParameterValue", "clip");
    }

    @Test
    public void testClipInvalidWkt() throws Exception {
        String point = "POINT(0";
        MockHttpServletResponse response = getAsServletResponse(
                "wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=sf__world&outputCrs=EPSG:3857&clip="
                        + point);
        checkOws20Exception(response, 400, "InvalidParameterValue", "clip");
    }

    @Test
    public void testClipOutside() throws Exception {
        // rectangle over europe, but layer is covering tasmania
        String europe = "POLYGON((35 -10, 70 -10, 70 40, 70 -10, 35 -10))";
        MockHttpServletResponse response = getAsServletResponse(
                "wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=BlueMarble&clip=" + europe);
        String message = checkOws20Exception(response, 400, "InvalidParameterValue", "clip");
        assertEquals("Clip polygon does not overlap coverage data", message);
    }

    public interface CoverageChecker {
        void check(GeoTiffReader reader, GridCoverage2D coverage) throws Exception;
    }

    private void testCoverage(MockHttpServletResponse response, String fileName, CoverageChecker checker)
            throws Exception {
        // got back a tiff
        assertEquals("image/tiff", response.getContentType());
        assertEquals(200, response.getStatus());

        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile(fileName, fileName + ".tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // check the tiff structure is the one requested
        final GeoTiffReader reader = new GeoTiffReader(file);
        GridCoverage2D coverage = null;
        try {
            coverage = reader.read(null);
            checker.check(reader, coverage);
        } finally {
            clean(reader, coverage);
        }
    }
}
