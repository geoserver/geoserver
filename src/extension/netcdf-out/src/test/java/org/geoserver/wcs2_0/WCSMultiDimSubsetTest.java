/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import static org.geotools.gml2.SrsSyntax.OGC_HTTP_URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wcs.CoverageCleanerCallback;
import org.geoserver.wcs2_0.response.GranuleStack;
import org.geotools.api.coverage.grid.GridCoverageReader;
import org.geotools.api.geometry.BoundingBox;
import org.geotools.api.parameter.ParameterValue;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class WCSMultiDimSubsetTest extends WCSNetCDFBaseTest {

    private static final QName LAMBERTMOSAIC = new QName(CiteTestData.WCS_URI, "lambert", CiteTestData.WCS_PREFIX);

    /* Test data for spatial sub-setting with native CRS != declared CRS
     * This MSG IR 12.0 image has EPSG:4087 - WGS 84 / World Equidistant Cylindrical CRS,
     * and we will set up the catalog, so that the declared CRS is EPSG:4326, then we
     * set ProjectionPolicy.REPROJECT_TO_DECLARED so that incoming requests should be
     * reprojected onto native CRS for WCS processing.
     * See https://osgeo-org.atlassian.net/browse/GEOS-11820 for details.
     */
    private static final QName SAT_MOSAIC = new QName(CiteTestData.WCS_URI, "sat", CiteTestData.WCS_PREFIX);

    @BeforeClass
    public static void init() {
        WCSNetCDFBaseTest.init();
        System.setProperty("testdata.force.delete", "true");
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // workaround to add our custom multidimensional format

        super.onSetUp(testData);

        testData.addRasterLayer(LAMBERTMOSAIC, "lambertmosaic.zip", null, null, this.getClass(), getCatalog());
        String coverageName = getLayerId(LAMBERTMOSAIC);
        setupRasterDimension(coverageName, ResourceInfo.TIME, DimensionPresentation.LIST, null);
        CoverageInfo info = getCatalog().getCoverageByName(coverageName);
        // Add this to prevent resource locking due to deferred disposal
        info.getParameters().put("USE_JAI_IMAGEREAD", "false");
        getCatalog().save(info);

        /* MSG (Europe) IR 12.0 image using EPSG:4087 - WGS 84 / World Equidistant Cylindrical CRS */
        testData.addRasterLayer(
                SAT_MOSAIC, "MSG_Europe_IR120_202412061315-geotiff.zip", null, null, this.getClass(), getCatalog());
        coverageName = getLayerId(SAT_MOSAIC);
        setupRasterDimension(coverageName, ResourceInfo.TIME, DimensionPresentation.LIST, null);
        info = getCatalog().getCoverageByName(coverageName);
        // Add this to prevent resource locking due to deferred disposal
        info.getParameters().put("USE_JAI_IMAGEREAD", "false");
        getCatalog().save(info);
    }

    /** Tests if we can select a single pixel value using a WCS request */
    @Test
    public void sliceLambert() throws Exception {

        // check we can read it as a TIFF and it is similare to the original one
        GridCoverage2D targetCoverage = null, sourceCoverage = null;
        GridCoverageReader coverageReader = null;
        try {

            // === slicing on Y axis
            // source
            CoverageInfo coverageInfo = this.getCatalog().getCoverageByName(LAMBERTMOSAIC.getLocalPart());
            coverageReader = coverageInfo.getGridCoverageReader(null, null);
            final ParameterValue<Boolean> useJAI = ImageMosaicFormat.USE_JAI_IMAGEREAD.createValue();
            useJAI.setValue(false);
            sourceCoverage = (GridCoverage2D) coverageReader.read(useJAI);
            final ReferencedEnvelope sourceEnvelope = sourceCoverage.getEnvelope2D();

            // subsample using the original extension
            MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1"
                    + "&coverageId=wcs__lambert&&Format=application/custom"
                    + "&subset=E,http://www.opengis.net/def/crs/EPSG/0/31300("
                    + sourceEnvelope.getMinX()
                    + ","
                    + (sourceEnvelope.getMinX() + 25)
                    + ")"
                    + "&subset=N,http://www.opengis.net/def/crs/EPSG/0/31300("
                    + sourceEnvelope.getMinY()
                    + ","
                    + (sourceEnvelope.getMinY() + 25)
                    + ")");

            assertNotNull(response);
            targetCoverage =
                    applicationContext.getBean(WCSResponseInterceptor.class).getLastResult();

            assertTrue(CRS.equalsIgnoreMetadata(
                    sourceCoverage.getCoordinateReferenceSystem(), targetCoverage.getCoordinateReferenceSystem()));

            assertTrue(targetCoverage instanceof GranuleStack);
            GridCoverage2D firstResult =
                    ((GranuleStack) targetCoverage).getGranules().get(0);

            // checks
            assertEquals(1, firstResult.getGridGeometry().getGridRange().getSpan(0));
            assertEquals(1, firstResult.getGridGeometry().getGridRange().getSpan(1));
            assertEquals(0, firstResult.getGridGeometry().getGridRange().getLow(0));
            assertEquals(1, firstResult.getGridGeometry().getGridRange().getLow(1));

        } finally {
            cleanCoverages(coverageReader, targetCoverage, sourceCoverage);
        }
    }

    /**
     * Tests if we can do spatial sub-setting in-case native coverage CRS != declared CRS. The SAT data is on EPSG:4087
     * - WGS 84 / World Equidistant Cylindrical, but the catalog is set up for EPSG:4326 as declared CRS, and
     * {@link ProjectionPolicy#REPROJECT_TO_DECLARED} is enabled.
     *
     * <p>Then, when we make WCS requests for spatial sub-setting on EPSG:3857, we should expect this to work properly
     * and the output then on EPSG:4326 if defined as output CRS.
     *
     * <p>See: <a href="https://osgeo-org.atlassian.net/browse/GEOS-11820">GEOS-11820</a> for details.
     */
    @Test
    public void sliceSATWithSubsettingCRSotherThanNativeCRS() throws Exception {

        CoordinateReferenceSystem outputCRS = CRS.decode("EPSG:4326");
        CoordinateReferenceSystem requestCRS = CRS.decode("EPSG:3857");

        ReferencedEnvelope requestEnvelope = new ReferencedEnvelope(7.0, 13.0, 45.0, 55.0, outputCRS);

        ReferencedEnvelope subSettingEnvelope = new ReferencedEnvelope(CRS.transform(requestEnvelope, requestCRS));

        // build the WCS request including spatial sub-setting
        String wcsRequest = "ows?request=GetCoverage&service=WCS&version=2.0.1"
                + "&coverageId=wcs__sat"
                + "&subsettingCRS=" + OGC_HTTP_URI.getSRS("EPSG:3857")
                + "&subset=X(" + subSettingEnvelope.getMinX() + "," + subSettingEnvelope.getMaxX() + ")"
                + "&subset=Y(" + subSettingEnvelope.getMinY() + "," + subSettingEnvelope.getMaxY() + ")"
                + "&outputCRS=" + OGC_HTTP_URI.getSRS("EPSG:4326")
                + "&format=application/custom";

        GridCoverage2D targetCoverage = null;
        try {
            // set up the layer so that 4326 is declared and REPROJECT_TO_DECLARED is enabled
            // this will require re-projection, because the native CRS of the data is EPSG:4087
            Catalog catalog = this.getCatalog();
            CoverageInfo coverageInfo = catalog.getCoverageByName(SAT_MOSAIC.getLocalPart());
            coverageInfo.setSRS("EPSG:4326");
            coverageInfo.setProjectionPolicy(ProjectionPolicy.REPROJECT_TO_DECLARED);
            catalog.save(coverageInfo);

            // fire off the request
            MockHttpServletResponse response = getAsServletResponse(wcsRequest);
            assertNotNull(response);
            assertEquals("WCS Response should have status OK == 200.", 200, response.getStatus());
            assertNotEquals(
                    "WCS response should not be XML, but was: \n" + response.getContentAsString(),
                    "application/xml",
                    response.getContentType());

            // get us the result
            targetCoverage =
                    applicationContext.getBean(WCSResponseInterceptor.class).getLastResult();

            assertTrue(
                    "Target coverage should have " + outputCRS.getName() + " CRS",
                    CRS.equalsIgnoreMetadata(outputCRS, targetCoverage.getCoordinateReferenceSystem()));

            assertTrue(targetCoverage instanceof GranuleStack);
            GridCoverage2D granule =
                    ((GranuleStack) targetCoverage).getGranules().get(0);
            ReferencedEnvelope granuleEnvelope = granule.getEnvelope2D();
            assertTrue(
                    "Granules should have " + outputCRS.getName() + " CRS",
                    CRS.equalsIgnoreMetadata(outputCRS, granuleEnvelope.getCoordinateReferenceSystem()));

            // TODO: it would be great to match the result envelope,
            //  but floating point precision makes it not an easy task
        } finally {
            cleanCoverages(null, targetCoverage, null);
        }
    }

    /**
     * Tests if we can do spatial sub-setting in-case native coverage CRS != declared CRS. The SAT data is on EPSG:4087
     * - WGS 84 / World Equidistant Cylindrical, but the catalog is set up for EPSG:4326 as declared CRS, and
     * {@link ProjectionPolicy#REPROJECT_TO_DECLARED} is enabled.
     *
     * <p>Then, when we make WCS requests for spatial sub-setting on EPSG:4326 as well as output CRS.
     *
     * <p>See: <a href="https://osgeo-org.atlassian.net/browse/GEOS-11820">GEOS-11820</a> for details.
     */
    @Test
    public void sliceSATWithOutputCRSDifferentThanNative() throws Exception {

        CoordinateReferenceSystem outputCRS = CRS.decode("EPSG:4326");
        BoundingBox requestEnvelope = new ReferencedEnvelope(7.0, 13.0, 45.0, 55.0, outputCRS);

        // build the WCS request including spatial sub-setting
        String wcsRequest = "ows?request=GetCoverage&service=WCS&version=2.0.1"
                + "&coverageId=wcs__sat"
                + "&subsettingCRS=" + OGC_HTTP_URI.getSRS("EPSG:4326")
                + "&subset=Long(" + requestEnvelope.getMinX() + "," + requestEnvelope.getMaxX() + ")"
                + "&subset=Lat(" + requestEnvelope.getMinY() + "," + requestEnvelope.getMaxY() + ")"
                + "&outputCRS=" + OGC_HTTP_URI.getSRS("EPSG:4326")
                + "&format=application/custom";

        GridCoverage2D targetCoverage = null;
        try {
            // set up the layer so that 4326 is declared and REPROJECT_TO_DECLARED is enabled
            // this will require re-projection, because the native CRS of the data is EPSG:4087
            Catalog catalog = this.getCatalog();
            CoverageInfo coverageInfo = catalog.getCoverageByName(SAT_MOSAIC.getLocalPart());
            coverageInfo.setSRS("EPSG:4326");
            coverageInfo.setProjectionPolicy(ProjectionPolicy.REPROJECT_TO_DECLARED);
            catalog.save(coverageInfo);

            // fire off the request
            MockHttpServletResponse response = getAsServletResponse(wcsRequest);
            assertNotNull(response);
            assertEquals("WCS Response should have status OK == 200.", 200, response.getStatus());
            assertNotEquals(
                    "WCS response should not be XML, but was: \n" + response.getContentAsString(),
                    "application/xml",
                    response.getContentType());

            // get us the result
            targetCoverage =
                    applicationContext.getBean(WCSResponseInterceptor.class).getLastResult();

            assertTrue(
                    "Target coverage should have " + outputCRS.getName() + " CRS",
                    CRS.equalsIgnoreMetadata(outputCRS, targetCoverage.getCoordinateReferenceSystem()));

            assertTrue(targetCoverage instanceof GranuleStack);
            GridCoverage2D granule =
                    ((GranuleStack) targetCoverage).getGranules().get(0);
            ReferencedEnvelope granuleEnvelope = granule.getEnvelope2D();
            assertTrue(
                    "Granules should have " + outputCRS.getName() + " CRS",
                    CRS.equalsIgnoreMetadata(outputCRS, granuleEnvelope.getCoordinateReferenceSystem()));

            // TODO: it would be great to match the result envelope,
            //  but floating point precision makes it not an easy task
        } finally {
            cleanCoverages(null, targetCoverage, null);
        }
    }
    /**
     * clean up after the test-case
     *
     * @param coverageReader the coverage reader
     * @param targetCoverage the target coverage
     * @param sourceCoverage the source coverage
     */
    private static void cleanCoverages(
            GridCoverageReader coverageReader, GridCoverage2D targetCoverage, GridCoverage2D sourceCoverage) {
        if (coverageReader != null) {
            try {
                coverageReader.dispose();
            } catch (Exception e) {
                // Ignore it
            }
        }
        if (targetCoverage != null) {
            try {
                CoverageCleanerCallback.disposeCoverage(targetCoverage);
            } catch (Exception e) {
                // Ignore it
            }
        }
        if (sourceCoverage != null) {
            try {
                CoverageCleanerCallback.disposeCoverage(sourceCoverage);
            } catch (Exception e) {
                // Ignore it
            }
        }
    }
}
