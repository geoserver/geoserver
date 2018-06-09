/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.kvp;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
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
import org.eclipse.emf.common.util.EList;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wcs2_0.WCS20Const;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataSourceException;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.wcs.v2_0.RangeSubset;
import org.geotools.wcs.v2_0.Scaling;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class GetCoverageKvpTest extends WCSKVPTestSupport {

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
    }

    @Test
    public void testParseBasic() throws Exception {
        GetCoverageType gc =
                parse("wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=theCoverage");

        assertEquals("theCoverage", gc.getCoverageId());
    }

    @Test
    public void testGetCoverageNoWs() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=BlueMarble&Format=image/tiff");

        assertEquals("image/tiff", response.getContentType());
    }

    @Test
    public void testGetCoverageNativeFormat() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=sf__rain");

        // we got back an ArcGrid response
        assertEquals("text/plain", response.getContentType());
    }

    @Test
    public void testNotExistent() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=NotThere&&Format=image/tiff");
        checkOws20Exception(response, 404, "NoSuchCoverage", "coverageId");
    }

    @Test
    public void testGetCoverageLocalWs() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs/wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=BlueMarble&&Format=image/tiff");

        assertEquals("image/tiff", response.getContentType());
    }

    @Test
    public void testExtensionScaleFactor() throws Exception {
        GetCoverageType gc =
                parse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=theCoverage&scaleFactor=2");

        Map<String, Object> extensions = getExtensionsMap(gc);

        assertEquals(1, extensions.size());
        ScalingType scaling = (ScalingType) extensions.get(Scaling.NAMESPACE + ":Scaling");
        ScaleByFactorType sbf = scaling.getScaleByFactor();
        assertEquals(2.0, sbf.getScaleFactor(), 0d);
    }

    @Test
    public void testExtensionScaleAxes() throws Exception {
        GetCoverageType gc =
                parse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
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
        GetCoverageType gc =
                parse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
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
        GetCoverageType gc =
                parse(
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
        GetCoverageType gc =
                parse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=theCoverage&rangesubset=band01,band03:band05,band10,band19:band21");

        Map<String, Object> extensions = getExtensionsMap(gc);

        assertEquals(1, extensions.size());
        RangeSubsetType rangeSubset =
                (RangeSubsetType) extensions.get(RangeSubset.NAMESPACE + ":RangeSubset");

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
        GetCoverageType gc =
                parse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=theCoverage&SUBSETTINGCRS=http://www.opengis.net/def/crs/EPSG/0/4326&outputcrs=http://www.opengis.net/def/crs/EPSG/0/32632");

        Map<String, Object> extensions = getExtensionsMap(gc);

        assertEquals(2, extensions.size());
        assertEquals(
                "http://www.opengis.net/def/crs/EPSG/0/4326",
                extensions.get(
                        "http://www.opengis.net/wcs/service-extension/crs/1.0:subsettingCrs"));
        assertEquals(
                "http://www.opengis.net/def/crs/EPSG/0/32632",
                extensions.get("http://www.opengis.net/wcs/service-extension/crs/1.0:outputCrs"));
    }

    @Test
    public void testExtensionInterpolationLinear() throws Exception {
        GetCoverageType gc =
                parse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=theCoverage&interpolation=http://www.opengis.net/def/interpolation/OGC/1/linear");

        Map<String, Object> extensions = getExtensionsMap(gc);

        InterpolationType interp =
                (InterpolationType)
                        extensions.get(
                                "http://www.opengis.net/WCS_service-extension_interpolation/1.0:Interpolation");
        assertEquals(
                "http://www.opengis.net/def/interpolation/OGC/1/linear",
                interp.getInterpolationMethod().getInterpolationMethod());
    }

    @Test
    public void testExtensionInterpolationMixed() throws Exception {
        GetCoverageType gc =
                parse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=theCoverage&interpolation=http://www.opengis.net/def/interpolation/OGC/1/linear");

        Map<String, Object> extensions = getExtensionsMap(gc);

        InterpolationType interp =
                (InterpolationType)
                        extensions.get(
                                "http://www.opengis.net/WCS_service-extension_interpolation/1.0:Interpolation");
        assertEquals(
                "http://www.opengis.net/def/interpolation/OGC/1/linear",
                interp.getInterpolationMethod().getInterpolationMethod());
    }

    @Test
    public void testExtensionOverview() throws Exception {
        GetCoverageType gc =
                parse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=theCoverage&overviewPolicy=QUALITY");
        Map<String, Object> extensions = getExtensionsMap(gc);

        assertEquals(1, extensions.size());
        String overviewPolicy =
                (String)
                        extensions.get(
                                WCS20Const.OVERVIEW_POLICY_EXTENSION_NAMESPACE
                                        + ":"
                                        + WCS20Const.OVERVIEW_POLICY_EXTENSION);
        assertEquals(overviewPolicy, "QUALITY");
    }

    @Test
    public void testGetMissingCoverage() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=notThereBaby");

        checkOws20Exception(response, 404, "NoSuchCoverage", "coverageId");
    }

    @Test
    public void testCqlFilterRed() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=sf__mosaic&CQL_FILTER=location like 'red%25'");
        assertOriginPixelColor(response, new int[] {255, 0, 0});
    }

    @Test
    public void testCqlFilterGreen() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=sf__mosaic&CQL_FILTER=location like 'green%25'");
        assertOriginPixelColor(response, new int[] {0, 255, 0});
    }

    @Test
    public void testSortByLocationAscending() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=sf__mosaic&sortBy=location");
        // green is the lowest, lexicographically
        assertOriginPixelColor(response, new int[] {0, 255, 0});
    }

    @Test
    public void testSortByLocationDescending() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=sf__mosaic&sortBy=location D");
        // yellow is the highest, lexicographically
        assertOriginPixelColor(response, new int[] {255, 255, 0});
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
}
