/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.wps.VerticalCRSConfigurationPanel;
import org.geoserver.wps.WPSTestSupport;
import org.geoserver.wps.gs.download.vertical.GeoTIFFVerticalGridShift;
import org.geoserver.wps.gs.download.vertical.VerticalGridTransform;
import org.geoserver.wps.gs.download.vertical.VerticalResampler;
import org.geoserver.wps.process.RawData;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.data.util.NullProgressListener;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.jts.WKTReader2;
import org.geotools.image.ImageWorker;
import org.geotools.process.ProcessException;
import org.geotools.referencing.CRS;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;

public class VerticalResampleTest extends WPSTestSupport {

    private static final double DELTA = 1E-6;

    private static QName HETEROGENEOUS_CRS2 = new QName(WCS_URI, "hcrs2", WCS_PREFIX);

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        // add limits properties file
        File dataDir = testData.getDataDirectoryRoot();
        File userProjections = new File(dataDir, "user_projections");
        if (!userProjections.exists()) {
            userProjections.mkdir();
        }
        testData.copyTo(
                VerticalResampleTest.class
                        .getClassLoader()
                        .getResourceAsStream("download-process/vertical/epsg.properties"),
                "user_projections/epsg.properties");
        testData.copyTo(
                VerticalResampleTest.class
                        .getClassLoader()
                        .getResourceAsStream("download-process/vertical/epsg_operations.properties"),
                "user_projections/epsg_operations.properties");
        testData.copyTo(
                VerticalResampleTest.class
                        .getClassLoader()
                        .getResourceAsStream("download-process/vertical/verticalgrid.tif"),
                "user_projections/verticalgrid.tif");
        testData.copyTo(
                VerticalResampleTest.class
                        .getClassLoader()
                        .getResourceAsStream("download-process/vertical/verticalgrid2.tif"),
                "user_projections/verticalgrid2.tif");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        testData.addRasterLayer(HETEROGENEOUS_CRS2, "heterogeneous_crs2.zip", null, getCatalog());
        CRS.reset("all");
        setVerticalCRS();
        final File file = new File(this.getTestData().getDataDirectoryRoot(), "user_projections/verticalgrid.tif");
        GeoTiffReader reader = new GeoTiffReader(file);
        reader.dispose();
    }

    private WPSResourceManager getResourceManager() {
        return GeoServerExtensions.bean(WPSResourceManager.class);
    }

    @Test
    public void testVerticalResamplingMissingSourceVerticalCRS() throws ParseException, FactoryException {
        final WPSResourceManager resourceManager = getResourceManager();
        GeoServer geoserver = getGeoServer();
        DownloadEstimatorProcess limits =
                new DownloadEstimatorProcess(new StaticDownloadServiceConfiguration(), geoserver);
        DownloadProcess downloadProcess = new DownloadProcess(geoserver, limits, resourceManager);
        unsetVerticalCRS();
        try {
            String roiWkt =
                    "POLYGON ((-102583.25 262175.25, -100000.25 262175.25, -100000.25 260300.25, -102583.25 260300.25, -102583.25 262175.25))";
            Polygon bboxRoi = (Polygon) new WKTReader2().read(roiWkt);
            CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:31256", true);
            Parameters parameters = new Parameters();
            List<Parameter> parametersList = parameters.getParameters();
            parametersList.add(new Parameter("writenodata", "false"));
            ProcessException e = Assert.assertThrows(ProcessException.class, new ThrowingRunnable() {

                @Override
                public void run() throws Throwable {
                    downloadProcess.execute(
                            getLayerId(HETEROGENEOUS_CRS2), // layerName
                            null, // filter
                            "image/tiff", // outputFormat
                            "image/tiff",
                            targetCRS, // targetCRS
                            targetCRS,
                            bboxRoi, // roi
                            false, // cropToGeometry
                            null, // interpolation
                            null, // targetSizeX
                            null, // targetSizeY
                            null, // bandSelectIndices
                            parameters, // Writing params
                            false,
                            false,
                            0d,
                            CRS.decode("EPSG:9999", true),
                            new NullProgressListener() // progressListener
                            );
                }
            });
            assertTrue(e.getMessage().contains("no source VerticalCRS"));
        } finally {
            // clean up process
            resourceManager.finished(resourceManager.getExecutionId(true));
            setVerticalCRS();
        }
    }

    @Test
    public void testVerticalResamplingOutsideOfTheGrid() throws ParseException, IOException, FactoryException {
        final WPSResourceManager resourceManager = GeoServerExtensions.bean(WPSResourceManager.class);
        GeoServer geoserver = getGeoServer();
        DownloadEstimatorProcess limits =
                new DownloadEstimatorProcess(new StaticDownloadServiceConfiguration(), geoserver);
        DownloadProcess downloadProcess = new DownloadProcess(geoserver, limits, resourceManager);

        // Requesting an area out of the vertical grid validity area. Pixels won't be modified

        String roiWkt =
                "POLYGON ((-101000.25 262175.25, -100000.25 262175.25, -100000.25 260300.25, -101000.25 260300.25, -101000.25 262175.25))";
        Polygon bboxRoi = (Polygon) new WKTReader2().read(roiWkt);
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:31256", true);
        Parameters parameters = new Parameters();
        List<Parameter> parametersList = parameters.getParameters();
        parametersList.add(new Parameter("writenodata", "false"));
        RawData raster = downloadProcess.execute(
                getLayerId(HETEROGENEOUS_CRS2), // layerName
                null, // filter
                "image/tiff", // outputFormat
                "image/tiff",
                targetCRS, // targetCRS
                targetCRS,
                bboxRoi, // roi
                false, // cropToGeometry
                null, // interpolation
                null, // targetSizeX
                null, // targetSizeY
                null, // bandSelectIndices
                parameters, // Writing params
                true,
                true,
                0d,
                CRS.decode("EPSG:9998", true),
                new NullProgressListener() // progressListener
                );
        try (DownloadProcessTest.AutoCloseableResource resource =
                        new DownloadProcessTest.AutoCloseableResource(resourceManager, raster);
                DownloadProcessTest.AutoDisposableGeoTiffReader reader =
                        new DownloadProcessTest.AutoDisposableGeoTiffReader(resource.getFile());
                DownloadProcessTest.AutoDisposableGridCoverage2D gc = reader.read()) {
            RenderedImage ri = gc.getRenderedImage();
            ImageWorker iw = new ImageWorker(ri);
            double[] minimum = iw.getMinimums();
            double[] maximum = iw.getMaximums();

            // The original file has only pixel with values in range [1 , 2]
            // Check that the vertical resampling didn't happen due to the vertical grid not
            // covering that area
            assertEquals(minimum[0], 1, DELTA);
            assertEquals(maximum[0], 2, DELTA);
        }
    }

    @Test
    public void testVerticalResampling() throws Exception {
        final WPSResourceManager resourceManager = getResourceManager();
        GeoServer geoserver = getGeoServer();
        DownloadEstimatorProcess limits =
                new DownloadEstimatorProcess(new StaticDownloadServiceConfiguration(), geoserver);
        DownloadProcess downloadProcess = new DownloadProcess(geoserver, limits, resourceManager);

        // Requesting an area containing a granule in native CRS and a granule in a different CRS

        String roiWkt =
                "POLYGON ((-102500.25 260000.25, -101000.25 260000.25, -101000.25 262500.25, -102500.25 262500.25, -102500.25 260000.25))";
        Polygon bboxRoi = (Polygon) new WKTReader2().read(roiWkt);
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:31256", true);
        Parameters parameters = new Parameters();
        List<Parameter> parametersList = parameters.getParameters();
        parametersList.add(new Parameter("writenodata", "false"));
        RawData raster = downloadProcess.execute(
                getLayerId(HETEROGENEOUS_CRS2), // layerName
                null, // filter
                "image/tiff", // outputFormat
                "image/tiff",
                targetCRS, // targetCRS
                targetCRS,
                bboxRoi, // roi
                false, // cropToGeometry
                null, // interpolation
                null, // targetSizeX
                null, // targetSizeY
                null, // bandSelectIndices
                parameters, // Writing params
                true,
                true,
                0d,
                CRS.decode("EPSG:9999", true),
                new NullProgressListener() // progressListener
                );
        try (DownloadProcessTest.AutoCloseableResource resource =
                        new DownloadProcessTest.AutoCloseableResource(resourceManager, raster);
                DownloadProcessTest.AutoDisposableGeoTiffReader reader =
                        new DownloadProcessTest.AutoDisposableGeoTiffReader(resource.getFile());
                DownloadProcessTest.AutoDisposableGridCoverage2D gc = reader.read()) {

            RenderedImage ri = gc.getRenderedImage();

            // The original file has only pixel with values in range [1 , 2]
            // On that ROI, the vertical grid has values around [60 , 160]
            // Grid values have been exaggerated for testing purposes

            // Check that the vertically interpolated values are coherent.
            // Not using equals check to allow some tolerance for the bilinear interpolation on the
            // grid values
            ImageWorker iw = new ImageWorker(ri);
            double[] minimum = iw.getMinimums();
            double[] maximum = iw.getMaximums();
            assertTrue(minimum[0] > 60);
            assertTrue(maximum[0] < 163);
        }
    }

    private void setVerticalCRS() {
        Catalog catalog = getCatalog();
        CoverageInfo coverageInfo = catalog.getCoverageByName(HETEROGENEOUS_CRS2.getLocalPart());
        coverageInfo.getMetadata().put(VerticalCRSConfigurationPanel.VERTICAL_CRS_KEY, "EPSG:5778");
        catalog.save(coverageInfo);
    }

    private void unsetVerticalCRS() {
        Catalog catalog = getCatalog();
        CoverageInfo coverageInfo = catalog.getCoverageByName(HETEROGENEOUS_CRS2.getLocalPart());
        coverageInfo.getMetadata().remove(VerticalCRSConfigurationPanel.VERTICAL_CRS_KEY);
        catalog.save(coverageInfo);
    }

    @AfterClass
    public static void cleanUp() {
        try {
            CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:5778", true);
            CoordinateReferenceSystem targetCRS1 = CRS.decode("EPSG:9998", true);
            CoordinateReferenceSystem targetCRS2 = CRS.decode("EPSG:9999", true);
            VerticalResampler resampler1 = new VerticalResampler(sourceCRS, targetCRS1, new GridCoverageFactory());
            VerticalResampler resampler2 = new VerticalResampler(sourceCRS, targetCRS2, new GridCoverageFactory());
            Field transform = VerticalResampler.class.getDeclaredField("verticalGridTransform");
            transform.setAccessible(true);
            VerticalGridTransform fieldValue1 = (VerticalGridTransform) transform.get(resampler1);
            VerticalGridTransform fieldValue2 = (VerticalGridTransform) transform.get(resampler2);
            GeoTIFFVerticalGridShift gtiff1 = (GeoTIFFVerticalGridShift) fieldValue1.getVerticalGridShift();
            GeoTIFFVerticalGridShift gtiff2 = (GeoTIFFVerticalGridShift) fieldValue2.getVerticalGridShift();
            gtiff1.dispose();
            gtiff2.dispose();

        } catch (FactoryException | NoSuchFieldException | IllegalAccessException e) {

        }
    }
}
